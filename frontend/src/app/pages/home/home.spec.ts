import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { PageEvent } from '@angular/material/paginator';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CommonModule } from '@angular/common';

describe('Home', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let productServiceMock: jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.fetchCurrentUser.and.returnValue(of({ id: '1', email: 'test@example.com', role: 'CLIENT' } as any));

    productServiceMock = jasmine.createSpyObj('ProductService', ['getAllProducts']);
    productServiceMock.getAllProducts.and.returnValue(of({
      content: [
        { id: '1', name: 'Product 1', price: 100, images: [] },
        { id: '2', name: 'Product 2', price: 200, images: [] }
      ],
      totalElements: 20,
      totalPages: 2,
      number: 0
    } as any));

    // Override component to use inline template
    TestBed.overrideComponent(HomeComponent, {
      set: {
        template: `
          <div *ngFor="let product of products">{{ product.name }} - {{ product.price }}</div>
          <app-product-card *ngFor="let product of products" [product]="product"></app-product-card>
        `,
        templateUrl: undefined
      }
    });

    await TestBed.configureTestingModule({
      imports: [HomeComponent, HttpClientTestingModule, CommonModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch current user on init', () => {
    fixture.detectChanges();

    expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
    expect(component.currentUser).toEqual({ id: '1', email: 'test@example.com', role: 'CLIENT' } as any);
  });

  it('should fetch products on init', () => {
    fixture.detectChanges();

    expect(productServiceMock.getAllProducts).toHaveBeenCalledWith(0, 10);
    expect(component.products.length).toBe(2);
    expect(component.totalElements).toBe(20);
  });

  it('should handle user fetch error', () => {
    authServiceMock.fetchCurrentUser.and.returnValue(throwError(() => ({ status: 500 })));
    spyOn(console, 'error');

    fixture.detectChanges();

    expect(console.error).toHaveBeenCalled();
    expect(component.errorMessage).toBe('Could not load user data.');
  });

  it('should handle page change event', () => {
    fixture.detectChanges();

    const pageEvent: PageEvent = { pageIndex: 1, pageSize: 20, length: 40 };
    component.onPageChange(pageEvent);

    expect(component.pageIndex).toBe(1);
    expect(component.pageSize).toBe(20);
    expect(productServiceMock.getAllProducts).toHaveBeenCalledWith(1, 20);
  });

  it('should refresh products after product deleted', () => {
    fixture.detectChanges();
    const initialCallCount = productServiceMock.getAllProducts.calls.count();

    component.onProductDeleted();

    expect(productServiceMock.getAllProducts.calls.count()).toBe(initialCallCount + 1);
  });

  it('should refresh products after product updated', () => {
    fixture.detectChanges();
    const initialCallCount = productServiceMock.getAllProducts.calls.count();

    component.onProductUpdated();

    expect(productServiceMock.getAllProducts.calls.count()).toBe(initialCallCount + 1);
  });

  it('should log product id on edit', () => {
    spyOn(console, 'log');

    component.onEdit('product-123');

    expect(console.log).toHaveBeenCalledWith('Edit (from home):', 'product-123');
  });

  it('should initialize with default pagination values', () => {
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(10);
    expect(component.totalElements).toBe(0);
  });

  it('should set products and totalElements from API response', () => {
    productServiceMock.getAllProducts.and.returnValue(of({
      content: [{ id: '1', name: 'Test', price: 50, images: [] }],
      totalElements: 100,
      totalPages: 10,
      number: 0
    } as any));

    component.fetchProducts();

    expect(component.products.length).toBe(1);
    expect(component.totalElements).toBe(100);
  });
});
