import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { MyProducts } from './my-products';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { PageEvent } from '@angular/material/paginator';
import { CommonModule } from '@angular/common';

xdescribe('MyProducts', () => {
  let component: MyProducts;
  let fixture: ComponentFixture<MyProducts>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let productServiceMock: jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.fetchCurrentUser.and.returnValue(of({ id: '1', email: 'test@example.com' } as any));

    productServiceMock = jasmine.createSpyObj('ProductService', ['getMyProducts']);
    productServiceMock.getMyProducts.and.returnValue(of({
      content: [
        { id: '1', name: 'Product 1', price: 100, images: [] },
        { id: '2', name: 'Product 2', price: 200, images: [] }
      ],
      totalElements: 20,
      totalPages: 2,
      number: 0
    } as any));

    // Override component to use inline template
    TestBed.overrideComponent(MyProducts, {
      set: {
        template: `
          <div *ngFor="let product of myProducts">
            {{ product.name }} - {{ product.price }}
          </div>
        `,
        styles: []
      }
    });

    await TestBed.configureTestingModule({
      imports: [MyProducts, HttpClientTestingModule, CommonModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MyProducts);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch products on init', () => {
    fixture.detectChanges();

    expect(productServiceMock.getMyProducts).toHaveBeenCalledWith(0, 10);
    expect(component.products.length).toBe(2);
    expect(component.totalElements).toBe(20);
  });

  it('should fetch current user on init', () => {
    fixture.detectChanges();

    expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
  });

  it('should initialize with default pagination values', () => {
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(10);
    expect(component.totalElements).toBe(0);
  });

  it('should handle page change event', () => {
    fixture.detectChanges();

    const pageEvent: PageEvent = { pageIndex: 1, pageSize: 20, length: 40 };
    component.onPageChange(pageEvent);

    expect(component.pageIndex).toBe(1);
    expect(component.pageSize).toBe(20);
    expect(productServiceMock.getMyProducts).toHaveBeenCalledWith(1, 20);
  });

  it('should refresh products after product deleted', () => {
    fixture.detectChanges();
    const initialCallCount = productServiceMock.getMyProducts.calls.count();

    component.onProductDeleted();

    expect(productServiceMock.getMyProducts.calls.count()).toBe(initialCallCount + 1);
  });

  it('should refresh products after product updated', () => {
    fixture.detectChanges();
    const initialCallCount = productServiceMock.getMyProducts.calls.count();

    component.onProductUpdated();

    expect(productServiceMock.getMyProducts.calls.count()).toBe(initialCallCount + 1);
  });

  it('should log product id on edit', () => {
    spyOn(console, 'log');

    component.onEdit('product-123');

    expect(console.log).toHaveBeenCalledWith('Edit product:', 'product-123');
  });

  it('should build full image URL', () => {
    const url = component.getImageUrl('/media/image.jpg');

    expect(url).toBe('https://localhost:8443/media/image.jpg');
  });

  it('should set products and totalElements from API response', () => {
    productServiceMock.getMyProducts.and.returnValue(of({
      content: [{ id: '1', name: 'Test', price: 50, images: [] }],
      totalElements: 100,
      totalPages: 10,
      number: 0
    } as any));

    component.fetchMyProducts();

    expect(component.products.length).toBe(1);
    expect(component.totalElements).toBe(100);
  });

  it('should handle empty product list', () => {
    productServiceMock.getMyProducts.and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0
    } as any));

    component.fetchMyProducts();

    expect(component.products.length).toBe(0);
    expect(component.totalElements).toBe(0);
  });

  it('should handle product fetch error', () => {
    productServiceMock.getMyProducts.and.returnValue(throwError(() => ({ status: 500 })));

    component.fetchMyProducts();

    expect(component.products.length).toBe(0);
  });

  it('should handle multiple page changes', () => {
    fixture.detectChanges();

    component.onPageChange({ pageIndex: 1, pageSize: 10, length: 40 } as PageEvent);
    expect(productServiceMock.getMyProducts).toHaveBeenCalledWith(1, 10);

    component.onPageChange({ pageIndex: 2, pageSize: 10, length: 40 } as PageEvent);
    expect(productServiceMock.getMyProducts).toHaveBeenCalledWith(2, 10);
  });

  it('should handle large page size', () => {
    fixture.detectChanges();

    const pageEvent: PageEvent = { pageIndex: 0, pageSize: 100, length: 500 };
    component.onPageChange(pageEvent);

    expect(component.pageSize).toBe(100);
    expect(productServiceMock.getMyProducts).toHaveBeenCalledWith(0, 100);
  });

  it('should handle negative page index gracefully', () => {
    fixture.detectChanges();

    const pageEvent: PageEvent = { pageIndex: -1, pageSize: 10, length: 40 };
    component.onPageChange(pageEvent);

    expect(component.pageIndex).toBe(-1);
    expect(productServiceMock.getMyProducts).toHaveBeenCalledWith(-1, 10);
  });

  it('should fetch products exactly once on init', () => {
    fixture.detectChanges();

    expect(productServiceMock.getMyProducts).toHaveBeenCalledTimes(1);
  });

  it('should log delete message on product deleted', () => {
    spyOn(console, 'log');

    component.onProductDeleted();

    expect(console.log).toHaveBeenCalledWith('Product deleted, refreshing list...');
  });

  it('should log update message on product updated', () => {
    spyOn(console, 'log');

    component.onProductUpdated();

    expect(console.log).toHaveBeenCalledWith('Product was updated, refreshing list...');
  });

  it('should handle products with different prices', () => {
    productServiceMock.getMyProducts.and.returnValue(of({
      content: [
        { id: '1', name: 'Cheap', price: 0.99, images: [] },
        { id: '2', name: 'Expensive', price: 9999.99, images: [] }
      ],
      totalElements: 2,
      totalPages: 1,
      number: 0
    } as any));

    component.fetchMyProducts();

    expect(component.products[0].price).toBe(0.99);
    expect(component.products[1].price).toBe(9999.99);
  });

  it('should handle image URL with empty path', () => {
    const url = component.getImageUrl('');

    expect(url).toBe('https://localhost:8443');
  });

  it('should handle image URL with root slash', () => {
    const url = component.getImageUrl('/image.jpg');

    expect(url).toBe('https://localhost:8443/image.jpg');
  });

  it('should handle image URL with multiple slashes', () => {
    const url = component.getImageUrl('/path/to/image.jpg');

    expect(url).toBe('https://localhost:8443/path/to/image.jpg');
  });

  it('should initialize products array as empty', () => {
    expect(component.products).toEqual([]);
  });

  it('should update pageIndex independently', () => {
    fixture.detectChanges();

    component.onPageChange({ pageIndex: 5, pageSize: 10, length: 100 } as PageEvent);
    expect(component.pageIndex).toBe(5);

    component.onPageChange({ pageIndex: 2, pageSize: 10, length: 100 } as PageEvent);
    expect(component.pageIndex).toBe(2);
  });

  it('should update pageSize independently', () => {
    fixture.detectChanges();

    component.onPageChange({ pageIndex: 0, pageSize: 25, length: 100 } as PageEvent);
    expect(component.pageSize).toBe(25);

    component.onPageChange({ pageIndex: 0, pageSize: 50, length: 100 } as PageEvent);
    expect(component.pageSize).toBe(50);
  });
});
