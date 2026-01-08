import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { forkJoin } from 'rxjs';
import { Order, OrderItem } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';

@Component({
    selector: 'app-cart',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatInputModule,
        MatFormFieldModule
    ],
    templateUrl: './cart.html',
    styleUrls: ['./cart.css']
})
export class Cart implements OnInit {
    cart: Order | null = null;
    isLoading = true;
    userId: string | null = null;
    productDetails: Record<string, ProductDetailDTO> = {};

    constructor(
        private orderService: OrderService,
        private authService: AuthService,
        private productService: ProductService,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.authService.currentUser$.subscribe(user => {
            if (user?.id) {
                this.userId = user.id;
                this.loadCart();
            }
        });

        // Subscribe to cart updates
        this.orderService.cart$.subscribe(cart => {
            this.cart = cart;
            if (cart) {
                this.populateProductDetails(cart.items);
            }
        });
    }

    loadCart(): void {
        if (!this.userId) return;

        this.isLoading = true;
        this.orderService.loadCart(this.userId).subscribe({
            next: () => {
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to load cart:', err);
                this.isLoading = false;
            }
        });
    }

    updateQuantity(item: OrderItem, newQuantity: number): void {
        const quantity = Number(newQuantity);
        if (!this.cart || !Number.isFinite(quantity) || quantity < 1) {
            return;
        }

        // Check stock availability
        this.productService.getProductById(item.productId).subscribe({
            next: (product) => {
                if (quantity > product.quantity) {
                    alert(`Only ${product.quantity} items available in stock`);
                    return;
                }

                const updatedItem: OrderItem = {
                    productId: item.productId,
                    quantity: quantity
                };

                this.orderService.updateOrderItem(this.cart!.id, item.productId, updatedItem).subscribe({
                    error: (err) => {
                        console.error('Failed to update item:', err);
                        alert('Failed to update quantity');
                    }
                });
            },
            error: (err) => {
                console.error('Failed to check stock:', err);
                alert('Failed to verify stock availability');
            }
        });
    }

    removeItem(productId: string): void {
        if (!this.cart) return;

        if (confirm('Remove this item from cart?')) {
            this.orderService.removeItemFromOrder(this.cart.id, productId).subscribe({
                error: (err) => {
                    console.error('Failed to remove item:', err);
                    alert('Failed to remove item');
                }
            });
        }
    }

    clearCartItems(): void {
        if (!this.cart || this.cart.items.length === 0) {
            return;
        }

        if (confirm('Remove all items from cart?')) {
            this.orderService.clearCartItems(this.cart.id).subscribe({
                error: (err) => {
                    console.error('Failed to clear cart:', err);
                    alert('Failed to clear cart');
                }
            });
        }
    }

    getTotal(): number {
        return this.cart ? this.cart.items.reduce((total, item) => total + this.getItemSubtotal(item), 0) : 0;
    }

    getImageUrl(productId: string): string {
        const detail = this.productDetails[productId];
        if (detail && detail.media && detail.media.length > 0) {
            return detail.media[0].fileUrl;
        }
        return 'https://localhost:8443/api/media/files/placeholder.jpg';
    }

    proceedToCheckout(): void {
        // Navigate to checkout page or implement checkout flow
        alert('Checkout functionality coming soon!');
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
            error: (err) => console.error('Failed to fetch product details for cart items:', err)
        });
    }
}
