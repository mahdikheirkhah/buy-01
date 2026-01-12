import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserProfile {
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    avatarUrl?: string;
    totalSpent: number;
    totalOrders: number;
    lastOrderDate?: string;
    bestProductId?: string;
    bestProductName?: string;
    mostBoughtCategory?: string;
    loyaltyPoints: number;
    savedAddresses: string[];
    preferredPaymentMethod?: number;
    totalReviews: number;
    averageRating: number;
    createdAt: string;
    updatedAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class UserProfileService {

    private apiUrl = 'https://localhost:8443/api';

    constructor(private http: HttpClient) { }

    /**
     * Get current user's profile (authenticated user)
     */
    getUserProfile(): Observable<UserProfile> {
        return this.http.get<UserProfile>(`${this.apiUrl}/users/profile`);
    }

    /**
     * Get user statistics
     */
    getUserStatistics(userId: string): Observable<UserProfile> {
        return this.http.get<UserProfile>(`${this.apiUrl}/users/${userId}/statistics`);
    }

    /**
     * Update current user's profile
     */
    updateUserProfile(profile: Partial<UserProfile>): Observable<UserProfile> {
        return this.http.put<UserProfile>(`${this.apiUrl}/users/profile`, profile);
    }

    /**
     * Get user's active cart (pending order)
     */
    getUserCart(userId: string): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/orders/user/${userId}/cart`);
    }
}
