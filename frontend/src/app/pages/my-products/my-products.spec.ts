import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { MyProducts } from './my-products';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { PageEvent } from '@angular/material/paginator';

describe('MyProducts', () => {
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

    await TestBed.configureTestingModule({
      imports: [MyProducts, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
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
});
