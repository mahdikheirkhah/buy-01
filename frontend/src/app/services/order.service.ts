import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject, map, of, throwError } from 'rxjs';
import { tap, catchError, switchMap } from 'rxjs/operators';
import { Order, OrderItem, CreateOrderRequest, UpdateOrderStatusRequest, PaymentMethod, CheckoutRequest } from '../models/order.model';
import { Page } from './product-service';

@Injectable({
    providedIn: 'root'
})
export class OrderService {
    private orderApiUrl = 'https://localhost:8443/api/orders';

    // Cart management - holds the current pending order
    public cartSubject = new BehaviorSubject<Order | null>(null);
    public cart$ = this.cartSubject.asObservable();

    // Cart item count for badge
    public cartItemCount$ = this.cart$.pipe(
        map(cart => cart ? cart.items.length : 0)
    );

    constructor(private http: HttpClient) { }

    // ==================== ORDER CRUD ====================

    createOrder(request: CreateOrderRequest): Observable<Order> {
        return this.http.post<Order>(this.orderApiUrl, request, { withCredentials: true });
    }

    getOrderById(orderId: string): Observable<Order> {
        return this.http.get<Order>(`${this.orderApiUrl}/${orderId}`, { withCredentials: true });
    }

    getUserOrders(userId: string, page: number, size: number): Observable<Page<Order>> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        return this.http.get<Page<Order>>(`${this.orderApiUrl}/user/${userId}`, {
            withCredentials: true,
            params: params
        });
    }

    updateOrderStatus(orderId: string, request: UpdateOrderStatusRequest): Observable<Order> {
        return this.http.put<Order>(`${this.orderApiUrl}/${orderId}/status`, request, { withCredentials: true });
    }

    cancelOrder(orderId: string): Observable<void> {
        return this.http.delete<void>(`${this.orderApiUrl}/${orderId}`, { withCredentials: true });
    }

    redoOrder(orderId: string): Observable<Order> {
        return this.http.post<Order>(`${this.orderApiUrl}/${orderId}/redo`, {}, { withCredentials: true });
    }

    // ==================== ORDER ITEM MANAGEMENT ====================

    addItemToOrder(orderId: string, item: OrderItem): Observable<Order> {
        return this.http.post<Order>(`${this.orderApiUrl}/${orderId}/items`, item, { withCredentials: true })
            .pipe(tap(order => {
                if (order.status === 'PENDING') {
                    this.cartSubject.next(order);
                }
            }));
    }

    updateOrderItem(orderId: string, productId: string, item: OrderItem): Observable<Order> {
        return this.http.put<Order>(`${this.orderApiUrl}/${orderId}/items/${productId}`, item, { withCredentials: true })
            .pipe(tap(order => {
                if (order.status === 'PENDING') {
                    this.cartSubject.next(order);
                }
            }));
    }

    removeItemFromOrder(orderId: string, productId: string): Observable<Order> {
        return this.http.delete<Order>(`${this.orderApiUrl}/${orderId}/items/${productId}`, { withCredentials: true })
            .pipe(tap(order => {
                if (order.status === 'PENDING') {
                    this.cartSubject.next(order);
                }
            }));
    }

    checkoutOrder(orderId: string, request: CheckoutRequest): Observable<Order> {
        return this.http.post<Order>(`${this.orderApiUrl}/${orderId}/checkout`, request, { withCredentials: true })
            .pipe(tap(order => {
                if (order.status === 'PENDING') {
                    this.cartSubject.next(order);
                } else {
                    this.cartSubject.next(null);
                }
            }));
    }

    clearCartItems(orderId: string): Observable<Order> {
        return this.http.delete<Order>(`${this.orderApiUrl}/${orderId}/items`, { withCredentials: true })
            .pipe(tap(order => {
                if (order.status === 'PENDING') {
                    this.cartSubject.next(order);
                }
            }));
    }

    // ==================== CART HELPERS ====================

    /**
     * Load or create the user's current cart (PENDING order)
     */
    loadCart(userId: string): Observable<Order | null> {
        return this.getActiveCart(userId).pipe(
            tap(order => this.cartSubject.next(order))
        );
    }

    /**
     * Get or create a PENDING order for the cart
     */
    getOrCreateCart(userId: string, shippingAddress: string): Observable<Order> {
        return this.getActiveCart(userId).pipe(
            switchMap(existingCart => {
                if (existingCart) {
                    this.cartSubject.next(existingCart);
                    return of(existingCart);
                }

                return this.createOrder({
                    userId,
                    shippingAddress,
                    items: [],
                    paymentMethod: PaymentMethod.CARD
                }).pipe(
                    tap(newOrder => this.cartSubject.next(newOrder))
                );
            })
        );
    }

    getActiveCart(userId: string): Observable<Order | null> {
        return this.http.get<Order>(`${this.orderApiUrl}/user/${userId}/cart`, {
            withCredentials: true,
            observe: 'response'
        }).pipe(
            map(response => response.body ?? null),
            catchError(err => {
                if (err.status === 404 || err.status === 204) {
                    return of(null);
                }
                return throwError(() => err);
            })
        );
    }

    /**
     * Clear the cart from memory (call after checkout)
     */
    clearCart(): void {
        this.cartSubject.next(null);
    }

    /**
     * Get current cart value
     */
    getCurrentCart(): Order | null {
        return this.cartSubject.value;
    }

    /**
     * Get cart item count
     */
    getCartItemCount(): number {
        const cart = this.getCurrentCart();
        return cart ? cart.items.reduce((count, item) => count + item.quantity, 0) : 0;
    }
}
