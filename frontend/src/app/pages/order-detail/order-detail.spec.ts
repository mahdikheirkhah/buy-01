import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { OrderDetail } from './order-detail';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product-service';
import { Order, OrderStatus, PaymentMethod, RedoOrderResponse } from '../../models/order.model';
import { ProductDetailDTO } from '../../models/product.model';

describe('OrderDetail', () => {
    let component: OrderDetail;
    let fixture: ComponentFixture<OrderDetail>;
    let orderServiceSpy: jasmine.SpyObj<OrderService>;
    let productServiceSpy: jasmine.SpyObj<ProductService>;
    let router: Router;
    let paramMapSubject: Subject<any>;

    // Mock data
    const mockOrder: Order = {
        id: 'order-123',
        userId: 'user-456',
        shippingAddress: '123 Main St, City, Country',
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

    const mockProductDetail: ProductDetailDTO = {
        productId: 'prod-1',
        name: 'Test Product',
        description: 'A test product',
        price: 49.99,
        quantity: 10,
        sellerId: 'seller-1',
        sellerFirstName: 'John',
        sellerLastName: 'Doe',
        sellerEmail: 'john@example.com',
        createdByMe: false,
        media: [{ fileId: 'media-1', fileUrl: 'https://example.com/image.jpg', productId: 'prod-1' }]
    };

    const mockProductDetail2: ProductDetailDTO = {
        ...mockProductDetail,
        productId: 'prod-2',
        name: 'Another Product',
        price: 29.99,
        media: []
    };

    beforeEach(async () => {
        paramMapSubject = new Subject();
        orderServiceSpy = jasmine.createSpyObj('OrderService', ['getOrderById', 'redoOrder']);
        productServiceSpy = jasmine.createSpyObj('ProductService', ['getProductById']);

        await TestBed.configureTestingModule({
            imports: [
                OrderDetail,
                HttpClientTestingModule,
                BrowserAnimationsModule,
                RouterTestingModule.withRoutes([
                    { path: 'my-orders', component: OrderDetail },
                    { path: 'cart', component: OrderDetail }
                ])
            ],
            providers: [
                { provide: OrderService, useValue: orderServiceSpy },
                { provide: ProductService, useValue: productServiceSpy },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        paramMap: paramMapSubject.asObservable()
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(OrderDetail);
        component = fixture.componentInstance;
        router = TestBed.inject(Router);
    });

    // ============ Component Creation ============
    describe('Component Creation', () => {
        it('should create', () => {
            expect(component).toBeTruthy();
        });

        it('should have default values', () => {
            expect(component.order).toBeNull();
            expect(component.isLoading).toBe(true);
            expect(component.orderId).toBeNull();
            expect(component.productDetails).toEqual({});
        });
    });

    // ============ Initialization Tests ============
    describe('ngOnInit()', () => {
        it('should load order when id param is present', fakeAsync(() => {
            orderServiceSpy.getOrderById.and.returnValue(of(mockOrder));
            productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));

            fixture.detectChanges();
            paramMapSubject.next({ get: () => 'order-123' });
            tick();

            expect(component.orderId).toBe('order-123');
            expect(orderServiceSpy.getOrderById).toHaveBeenCalledWith('order-123');
        }));

        it('should not load order when id param is null', fakeAsync(() => {
            fixture.detectChanges();
            paramMapSubject.next({ get: () => null });
            tick();

            expect(orderServiceSpy.getOrderById).not.toHaveBeenCalled();
        }));
    });

    // ============ Load Order Detail Tests ============
    describe('loadOrderDetail()', () => {
        beforeEach(() => {
            component.orderId = 'order-123';
        });

        it('should load order successfully', fakeAsync(() => {
            orderServiceSpy.getOrderById.and.returnValue(of(mockOrder));
            productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));

            component.loadOrderDetail();
            tick();

            expect(component.order).toEqual(mockOrder);
            expect(component.isLoading).toBe(false);
        }));

        it('should populate product details after loading order', fakeAsync(() => {
            orderServiceSpy.getOrderById.and.returnValue(of(mockOrder));
            productServiceSpy.getProductById.and.callFake((id: string) => {
                if (id === 'prod-1') return of(mockProductDetail);
                return of(mockProductDetail2);
            });

            component.loadOrderDetail();
            tick();

            expect(productServiceSpy.getProductById).toHaveBeenCalled();
        }));

        it('should handle load order error', fakeAsync(() => {
            orderServiceSpy.getOrderById.and.returnValue(throwError(() => new Error('Not found')));
            spyOn(console, 'error');

            component.loadOrderDetail();
            tick();

            expect(component.isLoading).toBe(false);
            expect(console.error).toHaveBeenCalled();
        }));

        it('should not load if orderId is null', () => {
            component.orderId = null;
            component.loadOrderDetail();
            expect(orderServiceSpy.getOrderById).not.toHaveBeenCalled();
        });
    });

    // ============ Navigation Tests ============
    describe('goBack()', () => {
        it('should navigate to my-orders', () => {
            const navigateSpy = spyOn(router, 'navigate');
            component.goBack();
            expect(navigateSpy).toHaveBeenCalledWith(['/my-orders']);
        });
    });

    // ============ Reorder Tests ============
    describe('reorder()', () => {
        beforeEach(() => {
            spyOn(window, 'alert');
        });

        it('should show success message and navigate to cart', fakeAsync(() => {
            const response: RedoOrderResponse = {
                order: mockOrder,
                message: 'All items successfully added to cart',
                outOfStockProducts: [],
                partiallyFilledProducts: []
            };
            orderServiceSpy.redoOrder.and.returnValue(of(response));
            const navigateSpy = spyOn(router, 'navigate');

            component.reorder('order-123');
            tick();

            expect(orderServiceSpy.redoOrder).toHaveBeenCalledWith('order-123');
            expect(window.alert).toHaveBeenCalledWith('All items successfully added to cart');
            expect(navigateSpy).toHaveBeenCalledWith(['/cart']);
        }));

        it('should show out of stock products in alert', fakeAsync(() => {
            const response: RedoOrderResponse = {
                order: null,
                message: 'No items could be added to cart. All products are out of stock.',
                outOfStockProducts: ["'Product A' is out of stock"],
                partiallyFilledProducts: []
            };
            orderServiceSpy.redoOrder.and.returnValue(of(response));
            const navigateSpy = spyOn(router, 'navigate');

            component.reorder('order-123');
            tick();

            const alertCall = (window.alert as jasmine.Spy).calls.mostRecent().args[0];
            expect(alertCall).toContain('Out of stock');
            expect(alertCall).toContain('Product A');
            // Should not navigate when order is null
            expect(navigateSpy).not.toHaveBeenCalled();
        }));

        it('should show partially filled products in alert', fakeAsync(() => {
            const response: RedoOrderResponse = {
                order: mockOrder,
                message: 'Some items could not be fully added to cart',
                outOfStockProducts: [],
                partiallyFilledProducts: ["'Product A' has only 3 available instead of 5"]
            };
            orderServiceSpy.redoOrder.and.returnValue(of(response));
            const navigateSpy = spyOn(router, 'navigate');

            component.reorder('order-123');
            tick();

            const alertCall = (window.alert as jasmine.Spy).calls.mostRecent().args[0];
            expect(alertCall).toContain('Reduced quantities');
            expect(navigateSpy).toHaveBeenCalledWith(['/cart']);
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
        }));

        it('should handle generic error', fakeAsync(() => {
            orderServiceSpy.redoOrder.and.returnValue(throwError(() => new Error('Network error')));
            spyOn(console, 'error');

            component.reorder('order-123');
            tick();

            expect(window.alert).toHaveBeenCalledWith('Failed to recreate order');
        }));
    });

    // ============ Status Color Tests ============
    describe('getStatusColor()', () => {
        it('should return correct color for PENDING', () => {
            expect(component.getStatusColor(OrderStatus.PENDING)).toBe('accent');
        });

        it('should return correct color for SHIPPING', () => {
            expect(component.getStatusColor(OrderStatus.SHIPPING)).toBe('primary');
        });

        it('should return correct color for DELIVERED', () => {
            expect(component.getStatusColor(OrderStatus.DELIVERED)).toBe('accent');
        });

        it('should return correct color for CANCELLED', () => {
            expect(component.getStatusColor(OrderStatus.CANCELLED)).toBe('warn');
        });

        it('should return empty string for unknown status', () => {
            expect(component.getStatusColor('UNKNOWN' as OrderStatus)).toBe('');
        });
    });

    // ============ Total Calculation Tests ============
    describe('getTotal()', () => {
        it('should return 0 when no order', () => {
            component.order = null;
            expect(component.getTotal()).toBe(0);
        });

        it('should calculate total correctly', () => {
            component.order = mockOrder;
            component.productDetails = {
                'prod-1': { ...mockProductDetail, price: 10 },
                'prod-2': { ...mockProductDetail2, price: 20 }
            };

            // prod-1: 10 * 2 = 20, prod-2: 20 * 1 = 20
            expect(component.getTotal()).toBe(40);
        });
    });

    // ============ Image URL Tests ============
    describe('getImageUrl()', () => {
        it('should return media URL when available', () => {
            component.productDetails = { 'prod-1': mockProductDetail };
            expect(component.getImageUrl('prod-1')).toBe('https://example.com/image.jpg');
        });

        it('should return placeholder when no media', () => {
            component.productDetails = { 'prod-2': mockProductDetail2 };
            expect(component.getImageUrl('prod-2')).toBe('https://localhost:8443/api/media/files/placeholder.jpg');
        });

        it('should return placeholder when product not found', () => {
            expect(component.getImageUrl('unknown')).toBe('https://localhost:8443/api/media/files/placeholder.jpg');
        });
    });

    // ============ Product Details Tests ============
    describe('Product Details', () => {
        beforeEach(() => {
            component.productDetails = {
                'prod-1': mockProductDetail,
                'prod-2': mockProductDetail2
            };
        });

        it('should return product name when available', () => {
            expect(component.getProductName('prod-1')).toBe('Test Product');
        });

        it('should return Loading... when product not found', () => {
            expect(component.getProductName('unknown')).toBe('Loading...');
        });

        it('should return product price when available', () => {
            expect(component.getProductPrice('prod-1')).toBe(49.99);
        });

        it('should return 0 when product price not available', () => {
            expect(component.getProductPrice('unknown')).toBe(0);
        });

        it('should calculate item subtotal correctly', () => {
            const item = { productId: 'prod-1', quantity: 3 };
            expect(component.getItemSubtotal(item)).toBeCloseTo(149.97, 2);
        });
    });
});