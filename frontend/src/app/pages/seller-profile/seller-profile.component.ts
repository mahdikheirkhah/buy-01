import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SellerProfileService, SellerProfile } from '../../services/seller-profile.service';
import { AuthService } from '../../services/auth';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
    selector: 'app-seller-profile',
    standalone: true,
    imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatTabsModule,
        ReactiveFormsModule
    ],
    templateUrl: './seller-profile.component.html',
    styleUrls: ['./seller-profile.component.scss']
})
export class SellerProfileComponent implements OnInit {
    sellerProfile: SellerProfile | null = null;
    isEditMode = false;
    isLoading = false;
    isOwnProfile = false;
    profileForm!: FormGroup;
    currentUserId: string = '';
    sellerId: string = '';

    constructor(
        private sellerProfileService: SellerProfileService,
        private authService: AuthService,
        private route: ActivatedRoute,
        private fb: FormBuilder,
        private snackBar: MatSnackBar
    ) {
        // Get current user ID from auth service
        this.authService.currentUser$.subscribe(user => {
            this.currentUserId = user?.id || '';
        });
    }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            this.sellerId = params['sellerId'] || this.currentUserId;
            this.isOwnProfile = this.sellerId === this.currentUserId;
            this.initForm();
            this.loadProfile();
        });
    }

    initForm(): void {
        this.profileForm = this.fb.group({
            sellerName: ['', Validators.required],
            shopDescription: [''],
            totalRevenue: [{ value: 0, disabled: true }],
            totalSales: [{ value: 0, disabled: true }],
            totalCustomers: [{ value: 0, disabled: true }],
            averageRating: [{ value: 0, disabled: true }],
            deliveryRating: [{ value: 0, disabled: true }],
            communicationRating: [{ value: 0, disabled: true }]
        });
    }

    loadProfile(): void {
        this.isLoading = true;
        const request = this.isOwnProfile
            ? this.sellerProfileService.getSellerProfile()
            : this.sellerProfileService.getPublicSellerProfile(this.sellerId);

        request.subscribe({
            next: (profile) => {
                this.sellerProfile = profile;
                this.updateForm(profile);
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Failed to load seller profile:', error);
                this.snackBar.open('Failed to load seller profile', 'Close', { duration: 5000 });
                this.isLoading = false;
            }
        });
    }

    updateForm(profile: SellerProfile): void {
        this.profileForm.patchValue({
            sellerName: profile.sellerName,
            shopDescription: profile.shopDescription || '',
            totalRevenue: profile.totalRevenue,
            totalSales: profile.totalSales,
            totalCustomers: profile.totalCustomers,
            averageRating: profile.averageRating,
            deliveryRating: profile.deliveryRating,
            communicationRating: profile.communicationRating
        });
    }

    toggleEditMode(): void {
        if (!this.isOwnProfile) return;
        this.isEditMode = !this.isEditMode;
        if (!this.isEditMode && this.sellerProfile) {
            this.updateForm(this.sellerProfile);
        }
    }

    saveProfile(): void {
        if (this.profileForm.invalid) {
            this.snackBar.open('Please fill all required fields correctly', 'Close', { duration: 5000 });
            return;
        }

        this.isLoading = true;
        const updateData = {
            sellerName: this.profileForm.get('sellerName')?.value,
            shopDescription: this.profileForm.get('shopDescription')?.value
        };

        this.sellerProfileService.updateSellerProfile(updateData).subscribe({
            next: (updatedProfile) => {
                this.sellerProfile = updatedProfile;
                this.isEditMode = false;
                this.snackBar.open('Profile updated successfully', 'Close', { duration: 5000 });
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Failed to update profile:', error);
                this.snackBar.open('Failed to update profile', 'Close', { duration: 5000 });
                this.isLoading = false;
            }
        });
    }

    get formattedRevenue(): string {
        return this.sellerProfile ? `$${this.sellerProfile.totalRevenue.toFixed(2)}` : '$0.00';
    }

    get joinDateDisplay(): string {
        return this.sellerProfile ? new Date(this.sellerProfile.joinDate).toLocaleDateString() : 'N/A';
    }

    get lastSaleDateDisplay(): string {
        if (!this.sellerProfile?.lastSaleDate) {
            return 'No sales yet';
        }
        return new Date(this.sellerProfile.lastSaleDate).toLocaleDateString();
    }

    get verificationBadge(): string {
        return this.sellerProfile?.isVerified ? 'Verified' : 'Not Verified';
    }

    get statusBadge(): string {
        return this.sellerProfile?.isActive ? 'Active' : 'Inactive';
    }
}
