import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SellerProfile {
    sellerId: string;
    sellerName: string;
    shopLogoUrl?: string;
    shopDescription?: string;
    totalRevenue: number;
    totalSales: number;
    totalOrders: number;
    totalCustomers: number;
    bestSellingProductId?: string;
    bestSellingProductName?: string;
    bestSellingProductCount: number;
    averageRating: number;
    totalReviews: number;
    totalFiveStarReviews: number;
    isVerified: boolean;
    isActive: boolean;
    deliveryRating: number;
    communicationRating: number;
    returnRate: number;
    cancellationRate: number;
    joinDate: string;
    lastSaleDate?: string;
    categories: string[];
    followerCount: number;
    createdAt: string;
    updatedAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class SellerProfileService {

    private apiUrl = 'https://localhost:8443/api';

    constructor(private http: HttpClient) { }

    /**
     * Get current seller's profile (authenticated seller)
     */
    getSellerProfile(): Observable<SellerProfile> {
        return this.http.get<SellerProfile>(`${this.apiUrl}/sellers/profile`, { withCredentials: true });
    }

    /**
     * Get public seller profile (viewable by anyone)
     */
    getPublicSellerProfile(sellerId: string): Observable<SellerProfile> {
        return this.http.get<SellerProfile>(`${this.apiUrl}/sellers/${sellerId}/profile`, { withCredentials: true });
    }

    /**
     * Get seller statistics (current seller only)
     */
    getSellerStatistics(sellerId: string): Observable<SellerProfile> {
        return this.http.get<SellerProfile>(`${this.apiUrl}/sellers/${sellerId}/statistics`, { withCredentials: true });
    }

    /**
     * Update seller profile (current seller only)
     */
    updateSellerProfile(profile: Partial<SellerProfile>): Observable<SellerProfile> {
        return this.http.put<SellerProfile>(`${this.apiUrl}/sellers/profile`, profile, { withCredentials: true });
    }

    /**
     * Get all products for a seller
     */
    getSellerProducts(sellerId: string): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/products/seller/${sellerId}`);
    }

    /**
     * Get seller reviews
     */
    getSellerReviews(sellerId: string): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/sellers/${sellerId}/reviews`);
    }
}
