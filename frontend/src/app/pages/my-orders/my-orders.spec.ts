import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { MyOrders } from './my-orders';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { Order, OrderStatus, PaymentMethod, RedoOrderResponse } from '../../models/order.model';
import { Page } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';

describe('MyOrders', () => {
    let component: MyOrders;
    let fixture: ComponentFixture<MyOrders>;
    let orderServiceSpy: jasmine.SpyObj<OrderService>;
    let authServiceSpy: jasmine.SpyObj<AuthService>;
    let productServiceSpy: jasmine.SpyObj<ProductService>;

    // Mock data
    const mockUser = { id: 'user-123', email: 'test@example.com' };

    const mockOrder: Order = {
        id: 'order-123',
        userId: 'user-123',
        shippingAddress: '123 Main St',
        status: OrderStatus.PROCESSING,
        items: [
            { productId: 'prod-1', quantity: 2 },
            { productId: 'prod-2', quantity: 1 }
        ],
        paymentMethod: PaymentMethod.CARD,
        orderDate: '2026-02-06T10:00:00Z',
        createdAt: '2026-02-06T10:00:00Z',
        updatedAt: '2026-02-06T10:00:00Z'
    };

    const mockPendingOrder: Order = {
        ...mockOrder,
        id: 'order-pending',
        status: OrderStatus.PENDING
    };

    const mockOrdersPage: Page<Order> = {
        content: [mockOrder, mockPendingOrder],
        totalElements: 2,
        totalPages: 1,
        number: 0
    };

    const mockProductDetail: ProductDetailDTO = {
        productId: 'prod-1',
        name: 'Test Product',
        description: 'A test product',
        price: 29.99,
        quantity: 10,
        sellerId: 'seller-1',
        sellerFirstName: 'John',
        sellerLastName: 'Doe',
        sellerEmail: 'john@example.com',
        createdByMe: false,
        media: []
    };

    beforeEach(async () => {
        orderServiceSpy = jasmine.createSpyObj('OrderService', ['getUserOrders', 'redoOrder', 'cancelShippingOrder']);
        authServiceSpy = jasmine.createSpyObj('AuthService', [], {
            currentUser$: of(mockUser)
        });
        productServiceSpy = jasmine.createSpyObj('ProductService', ['getProductById']);

        await TestBed.configureTestingModule({
            imports: [
                MyOrders,
                HttpClientTestingModule,
                BrowserAnimationsModule,
                RouterTestingModule
            ],
            providers: [
                { provide: OrderService, useValue: orderServiceSpy },
                { provide: AuthService, useValue: authServiceSpy },
                { provide: ProductService, useValue: productServiceSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(MyOrders);
        component = fixture.componentInstance;
    });

    // ============ Component Creation ============
    describe('Component Creation', () => {
        it('should create', () => {
            orderServiceSpy.getUserOrders.and.returnValue(of(mockOrdersPage));
            productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));
            fixture.detectChanges();
            expect(component).toBeTruthy();
        });

        it('should have default values', () => {
            expect(component.orders).toEqual([]);
            expect(component.pageIndex).toBe(0);
            expect(component.pageSize).toBe(10);
            expect(component.totalElements).toBe(0);
            expect(component.isLoading).toBe(true);
        });
    });

    // ============ Initialization Tests ============
    describe('ngOnInit()', () => {
        it('should fetch orders on init when user is logged in', fakeAsync(() => {
            orderServiceSpy.getUserOrders.and.returnValue(of(mockOrdersPage));
            productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));

            fixture.detectChanges();
            tick();

            expect(component.userId).toBe('user-123');
            expect(orderServiceSpy.getUserOrders).toHaveBeenCalledWith('user-123', 0, 10);
        }));

        it('should filter out PENDING orders', fakeAsync(() => {
            orderServiceSpy.getUserOrders.and.returnValue(of(mockOrdersPage));
            productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));

            fixture.detectChanges();
            tick();

            expect(component.orders.length).toBe(1);
            expect(component.orders[0].status).not.toBe(OrderStatus.PENDING);
        }));
    });

    // ============ Fetch Orders Tests ============
    describe('fetchOrders()', () => {
        beforeEach(() => {
            component.userId = 'user-123';
        });

        it('should fetch orders successfully', fakeAsync(() => {
            orderServiceSpy.getUserOrders.and.returnValue(of(mockOrdersPage));
            productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));

            component.fetchOrders();
            tick();

            expect(component.isLoading).toBe(false);
            expect(component.totalElements).toBe(2);
        }));

        it('should handle fetch orders error', fakeAsync(() => {
            orderServiceSpy.getUserOrders.and.returnValue(throwError(() => new Error('Network error')));
            spyOn(console, 'error');

            component.fetchOrders();
            tick();

            expect(component.isLoading).toBe(false);
            expect(console.error).toHaveBeenCalled();
        }));

        it('should not fetch if userId is null', () => {
            component.userId = null;
            component.fetchOrders();
            expect(orderServiceSpy.getUserOrders).not.toHaveBeenCalled();
        });
    });

    // ============ Pagination Tests ============
    describe('onPageChange()', () => {
        beforeEach(() => {
            component.userId = 'user-123';
            orderServiceSpy.getUserOrders.and.returnValue(of(mockOrdersPage));
            productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));
        });

        it('should update page index and size', fakeAsync(() => {
            component.onPageChange({ pageIndex: 2, pageSize: 20, length: 100 });
            tick();

            expect(component.pageIndex).toBe(2);
            expect(component.pageSize).toBe(20);
        }));

        it('should refetch orders after page change', fakeAsync(() => {
            component.onPageChange({ pageIndex: 1, pageSize: 5, length: 50 });
            tick();

            expect(orderServiceSpy.getUserOrders).toHaveBeenCalledWith('user-123', 1, 5);
        }));
    });

    // ============ Status Color Tests ============
    describe('getStatusColor()', () => {
        it('should return correct color for PENDING', () => {
            expect(component.getStatusColor(OrderStatus.PENDING)).toBe('accent');
        });

        it('should return correct color for PROCESSING', () => {
            expect(component.getStatusColor(OrderStatus.PROCESSING)).toBe('primary');
        });

        it('should return correct color for SHIPPED', () => {
            expect(component.getStatusColor(OrderStatus.SHIPPED)).toBe('primary');
        });

        it('should return correct color for DELIVERED', () => {
            expect(component.getStatusColor(OrderStatus.DELIVERED)).toBe('success');
        });

        it('should return correct color for CANCELLED', () => {
            expect(component.getStatusColor(OrderStatus.CANCELLED)).toBe('error');
        });

        it('should return empty string for unknown status', () => {
            expect(component.getStatusColor('UNKNOWN' as OrderStatus)).toBe('');
        });
    });

    // ============ Reorder Tests ============
    describe('reorder()', () => {
        beforeEach(() => {
            spyOn(window, 'alert');
        });

        it('should show success message when all items added', fakeAsync(() => {
            const response: RedoOrderResponse = {
                order: mockOrder,
                message: 'All items successfully added to cart',
                outOfStockProducts: [],
                partiallyFilledProducts: []
            };
            orderServiceSpy.redoOrder.and.returnValue(of(response));

            component.reorder('order-123');
            tick();

            expect(orderServiceSpy.redoOrder).toHaveBeenCalledWith('order-123');
            expect(window.alert).toHaveBeenCalledWith('All items successfully added to cart');
        }));

        it('should show out of stock products in alert', fakeAsync(() => {
            const response: RedoOrderResponse = {
                order: null,
                message: 'No items could be added to cart. All products are out of stock.',
                outOfStockProducts: ["'Product A' is out of stock", "'Product B' is out of stock"],
                partiallyFilledProducts: []
            };
            orderServiceSpy.redoOrder.and.returnValue(of(response));

            component.reorder('order-123');
            tick();

            const alertCall = (window.alert as jasmine.Spy).calls.mostRecent().args[0];
            expect(alertCall).toContain('Out of stock');
            expect(alertCall).toContain('Product A');
            expect(alertCall).toContain('Product B');
        }));

        it('should show partially filled products in alert', fakeAsync(() => {
            const response: RedoOrderResponse = {
                order: mockOrder,
                message: 'Some items could not be fully added to cart',
                outOfStockProducts: [],
                partiallyFilledProducts: ["'Product A' has only 3 available instead of 5"]
            };
            orderServiceSpy.redoOrder.and.returnValue(of(response));

            component.reorder('order-123');
            tick();

            const alertCall = (window.alert as jasmine.Spy).calls.mostRecent().args[0];
            expect(alertCall).toContain('Reduced quantities');
            expect(alertCall).toContain('only 3 available');
        }));

        it('should show both out of stock and partial in alert', fakeAsync(() => {
            const response: RedoOrderResponse = {
                order: mockOrder,
                message: 'Some items could not be fully added to cart',
                outOfStockProducts: ["'Product B' is out of stock"],
                partiallyFilledProducts: ["'Product A' has only 2 available instead of 5"]
            };
            orderServiceSpy.redoOrder.and.returnValue(of(response));

            component.reorder('order-123');
            tick();

            const alertCall = (window.alert as jasmine.Spy).calls.mostRecent().args[0];
            expect(alertCall).toContain('Out of stock');
            expect(alertCall).toContain('Reduced quantities');
        }));

        it('should handle error with custom error body', fakeAsync(() => {
            const errorResponse = {
                error: {
                    message: 'No items could be added to cart',
                    outOfStockProducts: ["'Product A' is out of stock"]
                },
                status: 409
            };
            orderServiceSpy.redoOrder.and.returnValue(throwError(() => errorResponse));
            spyOn(console, 'error');

            component.reorder('order-123');
            tick();

            const alertCall = (window.alert as jasmine.Spy).calls.mostRecent().args[0];
            expect(alertCall).toContain('No items could be added');
            expect(alertCall).toContain('Out of stock');
        }));

        it('should handle generic error', fakeAsync(() => {
            orderServiceSpy.redoOrder.and.returnValue(throwError(() => new Error('Network error')));
            spyOn(console, 'error');

            component.reorder('order-123');
            tick();

            expect(window.alert).toHaveBeenCalledWith('Failed to recreate order');
        }));
    });

    // ============ Product Details Tests ============
    describe('Product Details', () => {
        beforeEach(() => {
            component.productDetails = {
                'prod-1': mockProductDetail
            };
        });

        it('should return product name when available', () => {
            expect(component.getProductName('prod-1')).toBe('Test Product');
        });

        it('should return Loading... when product not loaded', () => {
            expect(component.getProductName('prod-unknown')).toBe('Loading...');
        });

        it('should return product price when available', () => {
            expect(component.getProductPrice('prod-1')).toBe(29.99);
        });

        it('should return 0 when product price not available', () => {
            expect(component.getProductPrice('prod-unknown')).toBe(0);
        });

        it('should calculate item subtotal correctly', () => {
            const item = { productId: 'prod-1', quantity: 3 };
            expect(component.getItemSubtotal(item)).toBe(89.97);
        });
    });

    // ============ Total Amount Tests ============
    describe('getTotalAmount()', () => {
        beforeEach(() => {
            component.productDetails = {
                'prod-1': { ...mockProductDetail, price: 10 },
                'prod-2': { ...mockProductDetail, productId: 'prod-2', price: 20 }
            };
        });

        it('should calculate total amount correctly', () => {
            const order: Order = {
                ...mockOrder,
                items: [
                    { productId: 'prod-1', quantity: 2 }, // 10 * 2 = 20
                    { productId: 'prod-2', quantity: 3 }  // 20 * 3 = 60
                ]
            };

            expect(component.getTotalAmount(order)).toBe(80);
        });

        it('should return 0 for order with no items', () => {
            const order: Order = { ...mockOrder, items: [] };
            expect(component.getTotalAmount(order)).toBe(0);
        });
    });
});