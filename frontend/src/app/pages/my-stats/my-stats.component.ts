import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { UserProfileService, UserProfile } from '../../services/user-profile.service';
import { SellerProfileService, SellerProfile } from '../../services/seller-profile.service';
import { AuthService } from '../../services/auth';
import { MatSnackBar } from '@angular/material/snack-bar';
import { User } from '../../models/user.model';

@Component({
    selector: 'app-my-stats',
    standalone: true,
    imports: [CommonModule, RouterModule, MatCardModule, MatIconModule],
    templateUrl: './my-stats.component.html',
    styleUrls: ['./my-stats.component.scss']
})
export class MyStatsComponent implements OnInit {
    userProfile: UserProfile | null = null;
    sellerProfile: SellerProfile | null = null;
    currentUserRole: string = '';
    currentUser: User | null = null;
    isLoading = false;

    constructor(
        private userProfileService: UserProfileService,
        private sellerProfileService: SellerProfileService,
        private authService: AuthService,
        private snackBar: MatSnackBar
    ) { }

    ngOnInit(): void {
        // Subscribe to current user and load stats when user is available
        this.authService.currentUser$.subscribe(user => {
            this.currentUser = user;
            this.currentUserRole = user?.role || '';

            if (user && user.id) {
                this.loadStats();
            }
        });
    }

    loadStats(): void {
        if (!this.currentUser || !this.currentUser.id) {
            console.warn('No user available for loading stats');
            return;
        }

        this.isLoading = true;
        const userId = this.currentUser.id;
        const role = this.currentUser.role;

        console.log('[MyStats] Loading stats for user:', userId, 'role:', role);

        if (role === 'SELLER') {
            // Load seller stats
            this.sellerProfileService.getSellerStatistics(userId).subscribe({
                next: (profile) => {
                    console.log('[MyStats] Seller profile loaded:', profile);
                    this.sellerProfile = profile;
                    this.isLoading = false;
                },
                error: (error) => {
                    console.error('Failed to load seller stats:', error);
                    this.snackBar.open('Failed to load seller statistics', 'Close', { duration: 5000 });
                    this.isLoading = false;
                }
            });
        } else {
            // Load customer stats
            this.userProfileService.getUserStatistics(userId).subscribe({
                next: (profile) => {
                    console.log('[MyStats] User profile loaded:', profile);
                    this.userProfile = this.mapUserStats(profile);
                    this.isLoading = false;
                },
                error: (error) => {
                    console.error('Failed to load customer stats:', error);
                    this.snackBar.open('Failed to load statistics', 'Close', { duration: 5000 });
                    this.isLoading = false;
                }
            });
        }
    }

    private mapUserStats(raw: any): UserProfile {
        return {
            userId: raw.userId ?? this.currentUser?.id ?? '',
            firstName: raw.firstName ?? this.currentUser?.firstName ?? '',
            lastName: raw.lastName ?? this.currentUser?.lastName ?? '',
            email: raw.email ?? this.currentUser?.email ?? '',
            avatarUrl: raw.avatarUrl,
            totalSpent: Number(raw.totalSpent ?? 0),
            totalOrders: raw.totalOrders ?? 0,
            lastOrderDate: raw.lastOrderDate ?? raw.lastOrder ?? null,
            bestProductId: raw.bestProductId ?? raw.mostPurchasedProductId ?? null,
            bestProductName: raw.bestProductName ?? raw.mostPurchasedProductName ?? null,
            mostBoughtCategory: raw.mostBoughtCategory ?? raw.bestCategory ?? raw.favoriteCategory,
            loyaltyPoints: raw.loyaltyPoints ?? 0,
            savedAddresses: raw.savedAddresses ?? [],
            preferredPaymentMethod: raw.preferredPaymentMethod,
            totalReviews: raw.totalReviews ?? 0,
            averageRating: raw.averageRating ?? 0,
            createdAt: raw.createdAt ?? '',
            updatedAt: raw.updatedAt ?? ''
        } as UserProfile;
    }

    // Customer getters
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

    // Seller getters
    get formattedRevenue(): string {
        return this.sellerProfile ? `$${this.sellerProfile.totalRevenue.toFixed(2)}` : '$0.00';
    }

    get joinDateDisplay(): string {
        if (!this.sellerProfile?.joinDate) {
            return 'Recently joined';
        }
        return new Date(this.sellerProfile.joinDate).toLocaleDateString();
    }

    get lastSaleDateDisplay(): string {
        if (!this.sellerProfile?.lastSaleDate) {
            return 'No sales yet';
        }
        return new Date(this.sellerProfile.lastSaleDate).toLocaleDateString();
    }
}
