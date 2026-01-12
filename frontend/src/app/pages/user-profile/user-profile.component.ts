import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UserProfileService, UserProfile } from '../../services/user-profile.service';
import { AuthService } from '../../services/auth';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
    selector: 'app-user-profile',
    standalone: true,
    imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        ReactiveFormsModule
    ],
    templateUrl: './user-profile.component.html',
    styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit {
    userProfile: UserProfile | null = null;
    isEditMode = false;
    isLoading = false;
    profileForm!: FormGroup;
    currentUserId: string = '';

    constructor(
        private userProfileService: UserProfileService,
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
        this.initForm();
        this.loadProfile();
    }

    initForm(): void {
        this.profileForm = this.fb.group({
            firstName: ['', Validators.required],
            lastName: ['', Validators.required],
            email: ['', [Validators.required, Validators.email]],
            loyaltyPoints: [{ value: 0, disabled: true }],
            totalSpent: [{ value: 0, disabled: true }],
            averageRating: [{ value: 0, disabled: true }],
            preferredPaymentMethod: ['']
        });
    }

    loadProfile(): void {
        this.isLoading = true;
        this.userProfileService.getUserProfile().subscribe({
            next: (profile) => {
                this.userProfile = profile;
                this.updateForm(profile);
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Failed to load user profile:', error);
                this.snackBar.open('Failed to load profile', 'Close', { duration: 5000 });
                this.isLoading = false;
            }
        });
    }

    updateForm(profile: UserProfile): void {
        this.profileForm.patchValue({
            firstName: profile.firstName,
            lastName: profile.lastName,
            email: profile.email,
            loyaltyPoints: profile.loyaltyPoints,
            totalSpent: profile.totalSpent,
            averageRating: profile.averageRating,
            preferredPaymentMethod: profile.preferredPaymentMethod || ''
        });
    }

    toggleEditMode(): void {
        this.isEditMode = !this.isEditMode;
        if (!this.isEditMode && this.userProfile) {
            this.updateForm(this.userProfile);
        }
    }

    saveProfile(): void {
        if (this.profileForm.invalid) {
            this.snackBar.open('Please fill all required fields correctly', 'Close', { duration: 5000 });
            return;
        }

        this.isLoading = true;
        const updateData = {
            firstName: this.profileForm.get('firstName')?.value,
            lastName: this.profileForm.get('lastName')?.value,
            email: this.profileForm.get('email')?.value,
            preferredPaymentMethod: this.profileForm.get('preferredPaymentMethod')?.value || null
        };

        this.userProfileService.updateUserProfile(updateData).subscribe({
            next: (updatedProfile) => {
                this.userProfile = updatedProfile;
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

    get formattedTotalSpent(): string {
        return this.userProfile ? `$${this.userProfile.totalSpent.toFixed(2)}` : '$0.00';
    }

    get bestCategoryDisplay(): string {
        return this.userProfile?.mostBoughtCategory || 'Not available';
    }

    get lastOrderDisplay(): string {
        if (!this.userProfile?.lastOrderDate) {
            return 'No orders yet';
        }
        return new Date(this.userProfile.lastOrderDate).toLocaleDateString();
    }
}
