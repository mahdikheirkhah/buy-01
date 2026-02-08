import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { Order, OrderItem, OrderStatus, PaymentMethod, CreateOrderRequest, UpdateOrderStatusRequest, CheckoutRequest, RedoOrderResponse } from '../models/order.model';
import { Page } from './product-service';

describe('OrderService', () => {
    let service: OrderService;
    let httpMock: HttpTestingController;

    // Mock data
    const mockOrderItem: OrderItem = {
        productId: 'prod-123',
        quantity: 2
    };

    const mockOrder: Order = {
        id: 'order-123',
        userId: 'user-456',
        shippingAddress: '123 Main St',
        status: OrderStatus.PENDING,
        items: [mockOrderItem],
        paymentMethod: PaymentMethod.CARD,
        orderDate: '2026-02-06T10:00:00Z',
        createdAt: '2026-02-06T10:00:00Z',
        updatedAt: '2026-02-06T10:00:00Z',
        isRemoved: false
    };

    const mockRedoOrderResponse: RedoOrderResponse = {
        order: mockOrder,
        message: 'All items successfully added to cart',
        outOfStockProducts: [],
        partiallyFilledProducts: []
    };

    const mockOrdersPage: Page<Order> = {
        content: [mockOrder],
        totalElements: 1,
        totalPages: 1,
        number: 0
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [OrderService]
        });

        service = TestBed.inject(OrderService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    // ============ Service Creation ============
    describe('Service Creation', () => {
        it('should be created', () => {
            expect(service).toBeTruthy();
        });

        it('should have initial cart as null', () => {
            expect(service.getCurrentCart()).toBeNull();
        });

        it('should have initial cart item count as 0', () => {
            expect(service.getCartItemCount()).toBe(0);
        });
    });

    // ============ Order CRUD Tests ============
    describe('createOrder()', () => {
        it('should send POST request to create order', () => {
            const request: CreateOrderRequest = {
                userId: 'user-456',
                shippingAddress: '123 Main St',
                items: [mockOrderItem],
                paymentMethod: PaymentMethod.CARD
            };

            service.createOrder(request).subscribe(result => {
                expect(result).toEqual(mockOrder);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(request);
            expect(req.request.withCredentials).toBe(true);

            req.flush(mockOrder);
        });

        it('should handle create order error', () => {
            const request: CreateOrderRequest = {
                userId: 'user-456',
                shippingAddress: '123 Main St',
                items: [],
                paymentMethod: PaymentMethod.CARD
            };

            service.createOrder(request).subscribe({
                next: () => fail('should have failed'),
                error: (error) => {
                    expect(error.status).toBe(400);
                }
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders');
            req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });
        });
    });

    describe('getOrderById()', () => {
        it('should send GET request to fetch order by id', () => {
            service.getOrderById('order-123').subscribe(result => {
                expect(result).toEqual(mockOrder);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123');
            expect(req.request.method).toBe('GET');
            expect(req.request.withCredentials).toBe(true);

            req.flush(mockOrder);
        });

        it('should handle order not found', () => {
            service.getOrderById('nonexistent').subscribe({
                next: () => fail('should have failed'),
                error: (error) => {
                    expect(error.status).toBe(404);
                }
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/nonexistent');
            req.flush('Not Found', { status: 404, statusText: 'Not Found' });
        });
    });

    describe('getUserOrders()', () => {
        it('should send GET request with pagination params', () => {
            service.getUserOrders('user-456', 0, 10).subscribe(result => {
                expect(result).toEqual(mockOrdersPage);
            });

            const req = httpMock.expectOne(
                'https://localhost:8443/api/orders/user/user-456?page=0&size=10'
            );
            expect(req.request.method).toBe('GET');
            expect(req.request.withCredentials).toBe(true);

            req.flush(mockOrdersPage);
        });

        it('should handle different page sizes', () => {
            service.getUserOrders('user-456', 2, 5).subscribe();

            const req = httpMock.expectOne(
                'https://localhost:8443/api/orders/user/user-456?page=2&size=5'
            );
            req.flush(mockOrdersPage);
        });
    });

    describe('updateOrderStatus()', () => {
        it('should send PUT request to update order status', () => {
            const request: UpdateOrderStatusRequest = {
                status: OrderStatus.PROCESSING
            };
            const updatedOrder = { ...mockOrder, status: OrderStatus.PROCESSING };

            service.updateOrderStatus('order-123', request).subscribe(result => {
                expect(result.status).toBe(OrderStatus.PROCESSING);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/status');
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual(request);

            req.flush(updatedOrder);
        });
    });

    describe('cancelOrder()', () => {
        it('should send DELETE request to cancel order', () => {
            service.cancelOrder('order-123').subscribe();

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123');
            expect(req.request.method).toBe('DELETE');
            expect(req.request.withCredentials).toBe(true);

            req.flush(null);
        });
    });

    describe('cancelShippingOrder()', () => {
        it('should send DELETE request and handle success', () => {
            service.cancelShippingOrder('order-123').subscribe(result => {
                expect(result).toEqual({});
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123');
            expect(req.request.method).toBe('DELETE');
            expect(req.request.withCredentials).toBe(true);

            req.flush(null);
        });

        it('should extract error message on failure', () => {
            service.cancelShippingOrder('order-123').subscribe(result => {
                expect(result.error).toBe('Order can only be cancelled when in SHIPPING status');
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123');
            req.flush(
                { error: 'Order can only be cancelled when in SHIPPING status' },
                { status: 400, statusText: 'Bad Request' }
            );
        });
    });

    // ============ Redo Order Tests ============
    describe('redoOrder()', () => {
        it('should send POST request to redo order', () => {
            service.redoOrder('order-123').subscribe(result => {
                expect(result).toEqual(mockRedoOrderResponse);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/redo');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual({});
            expect(req.request.withCredentials).toBe(true);

            req.flush(mockRedoOrderResponse);
        });

        it('should handle successful redo with all items in stock', () => {
            const response: RedoOrderResponse = {
                order: mockOrder,
                message: 'All items successfully added to cart',
                outOfStockProducts: [],
                partiallyFilledProducts: []
            };

            service.redoOrder('order-123').subscribe(result => {
                expect(result.message).toBe('All items successfully added to cart');
                expect(result.order).toBeTruthy();
                expect(result.outOfStockProducts.length).toBe(0);
                expect(result.partiallyFilledProducts.length).toBe(0);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/redo');
            req.flush(response);
        });

        it('should handle redo with out of stock products', () => {
            const response: RedoOrderResponse = {
                order: null,
                message: 'No items could be added to cart. All products are out of stock.',
                outOfStockProducts: ["'Product A' is out of stock", "'Product B' is out of stock"],
                partiallyFilledProducts: []
            };

            service.redoOrder('order-123').subscribe(result => {
                expect(result.order).toBeNull();
                expect(result.outOfStockProducts.length).toBe(2);
                expect(result.outOfStockProducts[0]).toContain('Product A');
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/redo');
            req.flush(response);
        });

        it('should handle redo with partially filled products', () => {
            const partialOrder = { ...mockOrder, items: [{ productId: 'prod-123', quantity: 3 }] };
            const response: RedoOrderResponse = {
                order: partialOrder,
                message: 'Some items could not be fully added to cart',
                outOfStockProducts: [],
                partiallyFilledProducts: ["'Product A' has only 3 available instead of 5"]
            };

            service.redoOrder('order-123').subscribe(result => {
                expect(result.order).toBeTruthy();
                expect(result.partiallyFilledProducts.length).toBe(1);
                expect(result.partiallyFilledProducts[0]).toContain('only 3 available');
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/redo');
            req.flush(response);
        });

        it('should handle redo with mixed stock issues', () => {
            const response: RedoOrderResponse = {
                order: mockOrder,
                message: 'Some items could not be fully added to cart',
                outOfStockProducts: ["'Product B' is out of stock"],
                partiallyFilledProducts: ["'Product A' has only 2 available instead of 5"]
            };

            service.redoOrder('order-123').subscribe(result => {
                expect(result.outOfStockProducts.length).toBe(1);
                expect(result.partiallyFilledProducts.length).toBe(1);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/redo');
            req.flush(response);
        });

        it('should handle redo order error (409 Conflict)', () => {
            const errorResponse: RedoOrderResponse = {
                order: null,
                message: 'No items could be added to cart. All products are out of stock.',
                outOfStockProducts: ["'Product A' is out of stock"],
                partiallyFilledProducts: []
            };

            service.redoOrder('order-123').subscribe({
                next: () => fail('should have failed'),
                error: (error) => {
                    expect(error.status).toBe(409);
                    expect(error.error.message).toContain('out of stock');
                }
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/redo');
            req.flush(errorResponse, { status: 409, statusText: 'Conflict' });
        });

        it('should handle redo order not found', () => {
            service.redoOrder('nonexistent').subscribe({
                next: () => fail('should have failed'),
                error: (error) => {
                    expect(error.status).toBe(404);
                }
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/nonexistent/redo');
            req.flush('Not Found', { status: 404, statusText: 'Not Found' });
        });
    });

    // ============ Order Item Management Tests ============
    describe('addItemToOrder()', () => {
        it('should send POST request to add item', () => {
            const newItem: OrderItem = { productId: 'prod-456', quantity: 1 };
            const updatedOrder = {
                ...mockOrder,
                items: [...mockOrder.items, newItem]
            };

            service.addItemToOrder('order-123', newItem).subscribe(result => {
                expect(result.items.length).toBe(2);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/items');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(newItem);

            req.flush(updatedOrder);
        });

        it('should update cart subject when adding item to pending order', () => {
            const newItem: OrderItem = { productId: 'prod-456', quantity: 1 };

            service.addItemToOrder('order-123', newItem).subscribe();

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/items');
            req.flush(mockOrder);

            expect(service.getCurrentCart()).toEqual(mockOrder);
        });
    });

    describe('updateOrderItem()', () => {
        it('should send PUT request to update item', () => {
            const updatedItem: OrderItem = { productId: 'prod-123', quantity: 5 };
            const updatedOrder = {
                ...mockOrder,
                items: [updatedItem]
            };

            service.updateOrderItem('order-123', 'prod-123', updatedItem).subscribe(result => {
                expect(result.items[0].quantity).toBe(5);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/items/prod-123');
            expect(req.request.method).toBe('PUT');

            req.flush(updatedOrder);
        });
    });

    describe('removeItemFromOrder()', () => {
        it('should send DELETE request to remove item', () => {
            const updatedOrder = { ...mockOrder, items: [] };

            service.removeItemFromOrder('order-123', 'prod-123').subscribe(result => {
                expect(result.items.length).toBe(0);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/items/prod-123');
            expect(req.request.method).toBe('DELETE');

            req.flush(updatedOrder);
        });
    });

    describe('checkoutOrder()', () => {
        it('should send POST request to checkout', () => {
            const request: CheckoutRequest = {
                shippingAddress: '456 Oak Ave',
                paymentMethod: PaymentMethod.PAY_ON_DELIVERY
            };
            const checkedOutOrder = { ...mockOrder, status: OrderStatus.PROCESSING };

            service.checkoutOrder('order-123', request).subscribe(result => {
                expect(result.status).toBe(OrderStatus.PROCESSING);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/checkout');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(request);

            req.flush(checkedOutOrder);
        });

        it('should clear cart after successful checkout', () => {
            // First set up a cart
            service.cartSubject.next(mockOrder);
            expect(service.getCurrentCart()).toBeTruthy();

            const request: CheckoutRequest = {
                shippingAddress: '456 Oak Ave',
                paymentMethod: PaymentMethod.CARD
            };

            service.checkoutOrder('order-123', request).subscribe();

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/checkout');
            req.flush({ ...mockOrder, status: OrderStatus.PROCESSING });

            expect(service.getCurrentCart()).toBeNull();
        });
    });

    describe('clearCartItems()', () => {
        it('should send DELETE request to clear cart items', () => {
            const emptyOrder = { ...mockOrder, items: [] };

            service.clearCartItems('order-123').subscribe(result => {
                expect(result.items.length).toBe(0);
            });

            const req = httpMock.expectOne('https://localhost:8443/api/orders/order-123/items');
            expect(req.request.method).toBe('DELETE');

            req.flush(emptyOrder);
        });
    });

    // ============ Cart Helper Tests ============
    describe('Cart Helpers', () => {
        describe('loadCart()', () => {
            it('should load cart and update subject', () => {
                service.loadCart('user-456').subscribe(result => {
                    expect(result).toEqual(mockOrder);
                });

                const req = httpMock.expectOne('https://localhost:8443/api/orders/user/user-456/cart');
                req.flush(mockOrder);

                expect(service.getCurrentCart()).toEqual(mockOrder);
            });

            it('should handle no cart found (204)', () => {
                service.loadCart('user-456').subscribe(result => {
                    expect(result).toBeNull();
                });

                const req = httpMock.expectOne('https://localhost:8443/api/orders/user/user-456/cart');
                req.flush(null, { status: 204, statusText: 'No Content' });
            });
        });

        describe('getActiveCart()', () => {
            it('should return cart when exists', () => {
                service.getActiveCart('user-456').subscribe(result => {
                    expect(result).toEqual(mockOrder);
                });

                const req = httpMock.expectOne('https://localhost:8443/api/orders/user/user-456/cart');
                req.flush(mockOrder);
            });

            it('should return null when no cart (404)', () => {
                service.getActiveCart('user-456').subscribe(result => {
                    expect(result).toBeNull();
                });

                const req = httpMock.expectOne('https://localhost:8443/api/orders/user/user-456/cart');
                req.flush('Not Found', { status: 404, statusText: 'Not Found' });
            });
        });

        describe('getOrCreateCart()', () => {
            it('should return existing cart if available', () => {
                service.getOrCreateCart('user-456', '123 Main St').subscribe(result => {
                    expect(result).toEqual(mockOrder);
                });

                const req = httpMock.expectOne('https://localhost:8443/api/orders/user/user-456/cart');
                req.flush(mockOrder);

                // Should not make a create request
                httpMock.expectNone('https://localhost:8443/api/orders');
            });

            it('should create new cart if none exists', () => {
                service.getOrCreateCart('user-456', '123 Main St').subscribe(result => {
                    expect(result).toEqual(mockOrder);
                });

                // First request - check for existing cart
                const getReq = httpMock.expectOne('https://localhost:8443/api/orders/user/user-456/cart');
                getReq.flush(null, { status: 404, statusText: 'Not Found' });

                // Second request - create new cart
                const postReq = httpMock.expectOne('https://localhost:8443/api/orders');
                expect(postReq.request.method).toBe('POST');
                postReq.flush(mockOrder);
            });
        });

        describe('clearCart()', () => {
            it('should set cart subject to null', () => {
                service.cartSubject.next(mockOrder);
                expect(service.getCurrentCart()).toBeTruthy();

                service.clearCart();

                expect(service.getCurrentCart()).toBeNull();
            });
        });

        describe('getCartItemCount()', () => {
            it('should return 0 when no cart', () => {
                expect(service.getCartItemCount()).toBe(0);
            });

            it('should return total quantity of items', () => {
                const cartWithItems: Order = {
                    ...mockOrder,
                    items: [
                        { productId: 'prod-1', quantity: 3 },
                        { productId: 'prod-2', quantity: 2 }
                    ]
                };
                service.cartSubject.next(cartWithItems);

                expect(service.getCartItemCount()).toBe(5);
            });
        });
    });

    // ============ Observable Tests ============
    describe('Observables', () => {
        it('cart$ should emit cart changes', (done) => {
            const emittedValues: (Order | null)[] = [];

            service.cart$.subscribe(value => {
                emittedValues.push(value);
                if (emittedValues.length === 2) {
                    expect(emittedValues[0]).toBeNull();
                    expect(emittedValues[1]).toEqual(mockOrder);
                    done();
                }
            });

            service.cartSubject.next(mockOrder);
        });

        it('cartItemCount$ should emit item count changes', (done) => {
            const emittedCounts: number[] = [];

            service.cartItemCount$.subscribe(count => {
                emittedCounts.push(count);
                if (emittedCounts.length === 2) {
                    expect(emittedCounts[0]).toBe(0);
                    expect(emittedCounts[1]).toBe(1);
                    done();
                }
            });

            service.cartSubject.next(mockOrder);
        });
    });
});