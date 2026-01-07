import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Order, OrderItem } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';

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
        if (!this.cart || newQuantity < 1) return;

        // Check stock availability
        this.productService.getProductById(item.productId).subscribe({
            next: (product) => {
                if (newQuantity > product.quantity) {
                    alert(`Only ${product.quantity} items available in stock`);
                    return;
                }

                const updatedItem: OrderItem = {
                    ...item,
                    quantity: newQuantity
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

    getTotal(): number {
        return this.cart ? this.orderService.calculateTotal(this.cart.items) : 0;
    }

    getImageUrl(productId: string): string {
        // Placeholder - you might want to store image URLs in the order items
        return 'https://localhost:8443/api/media/files/placeholder.jpg';
    }

    proceedToCheckout(): void {
        // Navigate to checkout page or implement checkout flow
        alert('Checkout functionality coming soon!');
    }
}
