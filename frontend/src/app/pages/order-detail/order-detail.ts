import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { forkJoin } from 'rxjs';
import { Order, OrderItem, OrderStatus } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';

@Component({
    selector: 'app-order-detail',
    standalone: true,
    imports: [
        CommonModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatChipsModule
    ],
    templateUrl: './order-detail.html',
    styleUrls: ['./order-detail.css']
})
export class OrderDetail implements OnInit {
    order: Order | null = null;
    isLoading = true;
    orderId: string | null = null;
    productDetails: Record<string, ProductDetailDTO> = {};

    constructor(
        private orderService: OrderService,
        private productService: ProductService,
        private activatedRoute: ActivatedRoute,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.activatedRoute.paramMap.subscribe(params => {
            this.orderId = params.get('id');
            if (this.orderId) {
                this.loadOrderDetail();
            }
        });
    }

    loadOrderDetail(): void {
        if (!this.orderId) return;

        this.isLoading = true;
        this.orderService.getOrderById(this.orderId).subscribe({
            next: (order) => {
                this.order = order;
                if (order && order.items) {
                    this.populateProductDetails(order.items);
                }
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to load order:', err);
                this.isLoading = false;
            }
        });
    }

    goBack(): void {
        this.router.navigate(['/my-orders']);
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

                // Only navigate to cart if order was created
                if (response.order) {
                    this.router.navigate(['/cart']);
                }
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

    getStatusColor(status: OrderStatus): string {
        switch (status) {
            case OrderStatus.PENDING: return 'accent';
            case OrderStatus.SHIPPING: return 'primary';
            case OrderStatus.DELIVERED: return 'success';
            case OrderStatus.CANCELLED: return 'error';
            default: return '';
        }
    }

    getTotal(): number {
        return this.order ? this.order.items.reduce((total, item) => total + this.getItemSubtotal(item), 0) : 0;
    }

    getImageUrl(productId: string): string {
        const detail = this.productDetails[productId];
        if (detail && detail.media && detail.media.length > 0) {
            return detail.media[0].fileUrl;
        }
        return 'https://localhost:8443/api/media/files/placeholder.jpg';
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

    private populateProductDetails(items: OrderItem[]): void {
        const uniqueIds = Array.from(new Set(items.map(item => item.productId)));
        const idsToFetch = uniqueIds.filter(id => !this.productDetails[id]);

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
            error: (err) => console.error('Failed to fetch product details:', err)
        });
    }
}