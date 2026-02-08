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
    isSeller = false;

    constructor(
        private orderService: OrderService,
        private productService: ProductService,
        private activatedRoute: ActivatedRoute,
        private router: Router
    ) {
        // Check if user is a seller from session storage or localStorage
        const userRole = sessionStorage.getItem('userRole') || localStorage.getItem('userRole') || '';
        this.isSeller = userRole.includes('SELLER');
        console.log('[OrderDetail] Component initialized');
        console.log('[OrderDetail] userRole from storage:', userRole);
        console.log('[OrderDetail] isSeller:', this.isSeller);
    }

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

        console.log('[OrderDetail] Starting loadOrderDetail - orderId:', this.orderId, 'isSeller:', this.isSeller);
        this.isLoading = true;
        this.orderService.getOrderById(this.orderId).subscribe({
            next: (order) => {
                console.log('[OrderDetail] SUCCESS - Order loaded successfully:', order);
                console.log('[OrderDetail] Order has', order.items ? order.items.length : 0, 'items');
                console.log('[OrderDetail] Order type - Has shippingAddress:', !!order.shippingAddress, 'Has paymentMethod:', !!order.paymentMethod);
                this.order = order;
                if (order && order.items) {
                    this.populateProductDetails(order.items);
                }
                this.isLoading = false;
            },
            error: (err) => {
                console.error('[OrderDetail] ERROR - Failed to load order detail');
                console.error('[OrderDetail] Status:', err.status);
                console.error('[OrderDetail] Status text:', err.statusText);
                console.error('[OrderDetail] Message:', err.message);
                console.error('[OrderDetail] Error body:', err.error);
                console.error('[OrderDetail] Full error:', err);
                this.isLoading = false;

                if (err.status === 403) {
                    console.error('[OrderDetail] 403 FORBIDDEN - User does not have permission to view this order');
                    alert('Access Denied: You don\'t have permission to view this order.');
                } else if (err.status === 404) {
                    alert('Order not found.');
                } else {
                    alert('Failed to load order details. Please try again.');
                }
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

                // Update cart with the new order if items were added
                if (response.order && response.order.items && response.order.items.length > 0) {
                    this.orderService.cartSubject.next(response.order);
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

    cancelOrder(orderId: string): void {
        const confirmed = confirm('Are you sure you want to cancel this order?');
        if (!confirmed) {
            return;
        }

        this.orderService.cancelShippingOrder(orderId).subscribe({
            next: (response) => {
                if (response.error) {
                    alert('Cannot cancel order: ' + response.error);
                } else {
                    alert('Order cancelled successfully. Stock has been restored.');
                    // Reload order details to show updated status
                    this.loadOrderDetail();
                }
            },
            error: (err) => {
                console.error('Failed to cancel order:', err);
                alert('Failed to cancel order. Please try again.');
            }
        });
    }

    removeOrder(orderId: string): void {
        const confirmed = confirm('Are you sure you want to remove this order from history?');
        if (!confirmed) {
            return;
        }

        this.orderService.removeOrder(orderId).subscribe({
            next: (response) => {
                if (response.error) {
                    alert('Cannot remove order: ' + response.error);
                } else {
                    alert('Order removed from history successfully.');
                    // Navigate back to orders list
                    this.router.navigate(['/my-orders']);
                }
            },
            error: (err) => {
                console.error('Failed to remove order:', err);
                alert('Failed to remove order. Please try again.');
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

    getImageUrl(item: OrderItem): string {
        // Use imageUrl from order item if available
        if (item.imageUrl) {
            return `https://localhost:8443${item.imageUrl}`;
        }

        // Fallback to product details
        const detail = this.productDetails[item.productId];
        if (detail && detail.media && detail.media.length > 0) {
            return `https://localhost:8443${detail.media[0].fileUrl}`;
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
