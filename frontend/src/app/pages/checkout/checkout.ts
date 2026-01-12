import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription, forkJoin } from 'rxjs';
import { Order, OrderItem, PaymentMethod, CheckoutRequest } from '../../models/order.model';
import { ProductDetailDTO } from '../../models/product.model';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product-service';
import { AuthService } from '../../services/auth';

@Component({
    selector: 'app-checkout',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatButtonModule,
        MatIconModule,
        MatListModule,
        MatDividerModule,
        MatProgressSpinnerModule
    ],
    templateUrl: './checkout.html',
    styleUrls: ['./checkout.css']
})
export class Checkout implements OnInit, OnDestroy {
    checkoutForm: FormGroup;
    cart: Order | null = null;
    isProcessing = false;
    errorMessage: string | null = null;
    successMessage: string | null = null;
    paymentMethodOptions = Object.values(PaymentMethod);
    productDetails: Record<string, ProductDetailDTO> = {};

    private userId: string | null = null;
    private subscriptions = new Subscription();

    constructor(
        private fb: FormBuilder,
        private orderService: OrderService,
        private productService: ProductService,
        private authService: AuthService,
        private router: Router
    ) {
        this.checkoutForm = this.fb.group({
            shippingAddress: ['', [Validators.required, Validators.minLength(10)]],
            paymentMethod: [PaymentMethod.CARD, Validators.required]
        });
    }

    ngOnInit(): void {
        const userSub = this.authService.currentUser$.subscribe(user => {
            this.userId = user?.id || null;
            if (this.userId) {
                this.ensureCartLoaded();
            }
        });
        this.subscriptions.add(userSub);

        const cartSub = this.orderService.cart$.subscribe(cart => {
            this.cart = cart;
            if (!cart || cart.items.length === 0) {
                this.productDetails = {};
                return;
            }

            this.productDetails = {};
            this.checkoutForm.patchValue({
                shippingAddress: cart.shippingAddress || '',
                paymentMethod: cart.paymentMethod || PaymentMethod.CARD
            });
            this.populateProductDetails(cart.items);
        });
        this.subscriptions.add(cartSub);
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    get hasItems(): boolean {
        return !!this.cart && this.cart.items.length > 0;
    }

    get cartTotal(): number {
        if (!this.cart) {
            return 0;
        }
        return this.cart.items.reduce((total, item) => total + this.getItemSubtotal(item), 0);
    }

    onSubmit(): void {
        this.errorMessage = null;
        if (!this.cart || !this.hasItems) {
            this.errorMessage = 'Your cart is empty. Add items before checking out.';
            return;
        }

        if (this.checkoutForm.invalid) {
            this.checkoutForm.markAllAsTouched();
            return;
        }

        this.isProcessing = true;
        const request = this.checkoutForm.value as CheckoutRequest;
        this.orderService.checkoutOrder(this.cart.id, request).subscribe({
            next: (order) => {
                this.isProcessing = false;
                this.successMessage = 'Checkout successful! Redirecting to order history...';
                setTimeout(() => this.router.navigate(['/my-orders']), 1500);
            },
            error: (err) => {
                this.isProcessing = false;
                this.successMessage = null;
                
                // Extract error message from various response formats
                let errorMsg = 'Checkout failed due to an unexpected error. Please retry.';
                
                if (err.status === 400) {
                    // Try to get message from different response formats
                    if (err.error?.message) {
                        errorMsg = err.error.message;
                    } else if (err.error?.error) {
                        errorMsg = err.error.error;
                    } else if (typeof err.error === 'string') {
                        errorMsg = err.error;
                    }
                } else if (err.status === 0) {
                    errorMsg = 'Unable to connect to server. Please check your internet connection.';
                } else {
                    errorMsg = `Server error (${err.status}). Please try again later.`;
                }
                
                this.errorMessage = errorMsg;
                console.error('Checkout error:', err);
            }
        });
    }

    getItemSubtotal(item: OrderItem): number {
        const detail = this.productDetails[item.productId];
        const price = detail ? detail.price : 0;
        return price * item.quantity;
    }

    getProductName(productId: string): string {
        const detail = this.productDetails[productId];
        return detail ? detail.name : 'Loading...';
    }

    private ensureCartLoaded(): void {
        if (!this.userId || this.hasItems) {
            return;
        }
        const loadSub = this.orderService.loadCart(this.userId).subscribe({
            next: () => {
                // cart observable will emit via cart subscription
            },
            error: () => {
                this.errorMessage = 'Unable to load your cart. Please refresh and try again.';
            }
        });
        this.subscriptions.add(loadSub);
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
            error: () => {
                this.errorMessage = 'Unable to load product details. Please retry later.';
            }
        });
    }
}
