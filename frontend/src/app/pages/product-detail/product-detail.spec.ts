import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { ProductDetail } from './product-detail';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';

describe('ProductDetail', () => {
  let component: ProductDetail;
  let fixture: ComponentFixture<ProductDetail>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let productServiceMock: jasmine.SpyObj<ProductService>;
  let routerMock: jasmine.SpyObj<Router>;
  let dialogMock: jasmine.SpyObj<MatDialog>;
  let activatedRouteMock: any;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.fetchCurrentUser.and.returnValue(of({ id: '1', role: 'SELLER' } as any));

    productServiceMock = jasmine.createSpyObj('ProductService', ['getProductById', 'deleteProduct']);
    productServiceMock.getProductById.and.returnValue(of({
      productId: '123',
      name: 'Test Product',
      price: 99.99,
      description: 'Test description',
      stock: 10,
      media: [{ fileUrl: '/images/test.jpg' }]
    } as any));

    routerMock = jasmine.createSpyObj('Router', ['navigate']);
    dialogMock = jasmine.createSpyObj('MatDialog', ['open']);

    activatedRouteMock = {
      snapshot: {
        paramMap: {
          get: jasmine.createSpy('get').and.returnValue('123')
        }
      }
    };

    // Override component to use inline template
    TestBed.overrideComponent(ProductDetail, {
      set: {
        template: `
          <div *ngIf="product">
            <h1>{{ product.name }}</h1>
            <p>{{ product.description }}</p>
            <p>{{ product.price }}</p>
            <button (click)="onEdit()">Edit</button>
            <button (click)="onDelete()">Delete</button>
          </div>
        `,
        templateUrl: undefined
      }
    });

    await TestBed.configureTestingModule({
      imports: [ProductDetail, HttpClientTestingModule, CommonModule, MatDialogModule],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: MatDialog, useValue: dialogMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetail);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch product on init', () => {
    fixture.detectChanges();

    expect(productServiceMock.getProductById).toHaveBeenCalledWith('123');
    expect(component.product).toBeTruthy();
    expect(component.product?.name).toBe('Test Product');
  });

  it('should set first image as selected on init', () => {
    fixture.detectChanges();

    expect(component.selectedImageUrl).toBe('https://localhost:8443/images/test.jpg');
  });

  it('should set isLoading to false after successful fetch', () => {
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
  });

  it('should handle product fetch error', () => {
    productServiceMock.getProductById.and.returnValue(throwError(() => ({ status: 404 })));
    spyOn(console, 'error');

    fixture.detectChanges();

    expect(console.error).toHaveBeenCalled();
    expect(component.errorMessage).toBe('Could not load product details.');
    expect(component.isLoading).toBe(false);
  });

  it('should select image when selectImage is called', () => {
    component.selectImage('/images/new-image.jpg');

    expect(component.selectedImageUrl).toBe('https://localhost:8443/images/new-image.jpg');
  });

  it('should build full image URL correctly', () => {
    const fullUrl = component.getFullImageUrl('/media/image.png');

    expect(fullUrl).toBe('https://localhost:8443/media/image.png');
  });

  it('should open edit modal on onEdit', () => {
    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.product = { productId: '123', name: 'Test' } as any;

    component.onEdit();

    expect(dialogMock.open).toHaveBeenCalled();
  });

  it('should refresh product after successful edit', () => {
    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.product = { productId: '123', name: 'Test' } as any;
    spyOn(component, 'ngOnInit');

    component.onEdit();

    expect(component.ngOnInit).toHaveBeenCalled();
  });

  it('should not open edit modal if product is null', () => {
    component.product = null;

    component.onEdit();

    expect(dialogMock.open).not.toHaveBeenCalled();
  });

  it('should open delete confirmation on onDelete', () => {
    const dialogRefMock = { afterClosed: () => of(false) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.product = { productId: '123', name: 'Test' } as any;

    component.onDelete();

    expect(dialogMock.open).toHaveBeenCalled();
  });

  it('should delete product and navigate on confirmation', () => {
    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    productServiceMock.deleteProduct.and.returnValue(of('Product deleted successfully'));
    component.product = { productId: '123', name: 'Test' } as any;

    component.onDelete();

    expect(productServiceMock.deleteProduct).toHaveBeenCalledWith('123');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/my-products']);
  });

  it('should handle delete error', () => {
    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    productServiceMock.deleteProduct.and.returnValue(throwError(() => ({ status: 500 })));
    spyOn(console, 'error');
    component.product = { productId: '123', name: 'Test' } as any;

    component.onDelete();

    expect(console.error).toHaveBeenCalled();
  });

  it('should not delete if user cancels confirmation', () => {
    const dialogRefMock = { afterClosed: () => of(false) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.product = { productId: '123', name: 'Test' } as any;

    component.onDelete();

    expect(productServiceMock.deleteProduct).not.toHaveBeenCalled();
  });

  it('should not open delete modal if product is null', () => {
    component.product = null;

    component.onDelete();

    expect(dialogMock.open).not.toHaveBeenCalled();
  });
});
