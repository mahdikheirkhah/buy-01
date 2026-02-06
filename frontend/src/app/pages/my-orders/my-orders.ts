import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { Order, OrderItem, OrderStatus } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { Page } from '../../services/product-service';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';

@Component({
    selector: 'app-my-orders',
    standalone: true,
    imports: [
        CommonModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatPaginatorModule,
        MatChipsModule,
        MatIconModule
    ],
    templateUrl: './my-orders.html',
    styleUrls: ['./my-orders.css']
})
export class MyOrders implements OnInit {
    orders: Order[] = [];
    pageIndex = 0;
    pageSize = 10;
    totalElements = 0;
    isLoading = true;
    userId: string | null = null;
    productDetails: Record<string, ProductDetailDTO> = {};

    constructor(
        private orderService: OrderService,
        private authService: AuthService,
        private productService: ProductService
    ) { }

    ngOnInit(): void {
        this.authService.currentUser$.subscribe(user => {
            if (user?.id) {
                this.userId = user.id;
                this.fetchOrders();
            }
        });
    }

    fetchOrders(): void {
        if (!this.userId) return;

        this.isLoading = true;
        this.orderService.getUserOrders(this.userId, this.pageIndex, this.pageSize)
            .subscribe({
                next: (page: Page<Order>) => {
                    this.orders = page.content.filter(order => order.status !== OrderStatus.PENDING);
                    this.totalElements = page.totalElements;
                    this.populateProductDetails(this.orders);
                    this.isLoading = false;
                },
                error: (err) => {
                    console.error('Failed to fetch orders:', err);
                    this.isLoading = false;
                }
            });
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.fetchOrders();
    }

    getStatusColor(status: OrderStatus): string {
        switch (status) {
            case OrderStatus.PENDING: return 'accent';
            case OrderStatus.PROCESSING: return 'primary';
            case OrderStatus.SHIPPED: return 'primary';
            case OrderStatus.DELIVERED: return 'success';
            case OrderStatus.CANCELLED: return 'error';
            default: return '';
        }
    }

    getTotalAmount(order: Order): number {
        return order.items.reduce((total, item) => total + this.getItemSubtotal(item), 0);
    }

    reorder(orderId: string): void {
        this.orderService.redoOrder(orderId).subscribe({
            next: (response) => {
                let alertMessage = response.message;

                // Append details about stock issues
                if (response.outOfStockProducts && response.outOfStockProducts.length > 0) {
                    alertMessage += '\n\nOut of stock:\n• ' + response.outOfStockProducts.join('\n• ');
                }
                if (response.partiallyFilledProducts && response.partiallyFilledProducts.length > 0) {
                    alertMessage += '\n\nReduced quantities:\n• ' + response.partiallyFilledProducts.join('\n• ');
                }

                alert(alertMessage);
            },
            error: (err) => {
                console.error('Failed to reorder:', err);
                // Check if error response contains our custom error body
                if (err.error && err.error.message) {
                    let alertMessage = err.error.message;
                    if (err.error.outOfStockProducts && err.error.outOfStockProducts.length > 0) {
                        alertMessage += '\n\nOut of stock:\n• ' + err.error.outOfStockProducts.join('\n• ');
                    }
                    alert(alertMessage);
                } else {
                    alert('Failed to recreate order');
                }
            }
        });
    }

    getProductName(productId: string): string {
        const detail = this.productDetails[productId];
        return detail ? detail.name : 'Loading...';
    }

    getProductPrice(productId: string): number {
        const detail = this.productDetails[productId];
        return detail ? detail.price : 0;
    }

    getItemSubtotal(item: OrderItem): number {
        return this.getProductPrice(item.productId) * item.quantity;
    }

    private populateProductDetails(orders: Order[]): void {
        const uniqueIds = new Set<string>();
        orders.forEach(order => order.items.forEach(item => uniqueIds.add(item.productId)));

        const idsToFetch = Array.from(uniqueIds).filter(id => !this.productDetails[id]);
        if (idsToFetch.length === 0) {
            return;
        }

        forkJoin(idsToFetch.map(id => this.productService.getProductById(id))).subscribe({
            next: (products) => {
                products.forEach(product => {
                    const key = product.productId || product.id;
                    if (key) {
                        this.productDetails[key] = product;
                    }
                });
            },
            error: (err) => console.error('Failed to fetch product details for orders:', err)
        });
    }
}