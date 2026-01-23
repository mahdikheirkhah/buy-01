import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router, RouterLink } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ProductCard } from './product-card';
import { ProductService } from '../../services/product-service';
import { ProductCardDTO } from '../../models/productCard.model';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CurrencyPipe } from '@angular/common';

describe('ProductCard', () => {
  let component: ProductCard;
  let fixture: ComponentFixture<ProductCard>;
  let productServiceMock: jasmine.SpyObj<ProductService>;
  let dialogMock: jasmine.SpyObj<MatDialog>;
  let routerMock: jasmine.SpyObj<Router>;

  const mockProduct: ProductCardDTO = {
    id: 'prod-123',
    name: 'Test Product',
    description: 'A test product',
    price: 99.99,
    quantity: 10,
    createdByMe: true,
    imageUrls: ['/uploads/image1.jpg', '/uploads/image2.jpg', '/uploads/image3.jpg']
  };

  beforeEach(async () => {
    productServiceMock = jasmine.createSpyObj('ProductService', ['deleteProduct', 'updateProduct']);
    productServiceMock.deleteProduct.and.returnValue(of('Product deleted successfully'));

    dialogMock = jasmine.createSpyObj('MatDialog', ['open']);
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        ProductCard,
        HttpClientTestingModule,
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        CurrencyPipe,
        MatDialogModule,
        RouterLink
      ],
      providers: [
        { provide: ProductService, useValue: productServiceMock },
        { provide: MatDialog, useValue: dialogMock },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCard);
    component = fixture.componentInstance;
    component.product = mockProduct;
  });

  afterEach(() => {
    if (component.imageChangeInterval) {
      clearInterval(component.imageChangeInterval);
    }
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start image carousel on init with multiple images', fakeAsync(() => {
    component.ngOnInit();
    expect(component.currentImageIndex).toBe(0);

    tick(3000);
    expect(component.currentImageIndex).toBe(1);

    tick(3000);
    expect(component.currentImageIndex).toBe(2);

    tick(3000);
    expect(component.currentImageIndex).toBe(0); // Wraps around
  }));

  it('should not start carousel with single image', fakeAsync(() => {
    component.product = { ...mockProduct, imageUrls: ['/uploads/single.jpg'] };
    component.ngOnInit();

    tick(3000);
    expect(component.currentImageIndex).toBe(0);
  }));

  it('should clear interval on destroy', () => {
    component.ngOnInit();
    const intervalId = component.imageChangeInterval;

    component.ngOnDestroy();

    expect(component.imageChangeInterval).toBeNull();
  });

  it('should pause carousel on mouse enter', () => {
    component.ngOnInit();
    spyOn(window, 'clearInterval');

    component.onMouseEnter();

    expect(window.clearInterval).toHaveBeenCalled();
  });

  it('should restart carousel on mouse leave', () => {
    component.onMouseEnter(); // Stop first
    spyOn(component, 'startImageCarousel');

    component.onMouseLeave();

    expect(component.startImageCarousel).toHaveBeenCalled();
  });

  it('should build full image URL', () => {
    const url = component.getImageUrl('/uploads/test.jpg');

    expect(url).toBe('https://localhost:8443/uploads/test.jpg');
  });

  it('should navigate to product detail on card click', () => {
    component.onCardClick();

    expect(routerMock.navigate).toHaveBeenCalledWith(['/product', 'prod-123']);
  });

  it('should not navigate if product is null', () => {
    component.product = null;

    component.onCardClick();

    expect(routerMock.navigate).not.toHaveBeenCalled();
  });

  it('should open delete confirmation dialog', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(mockEvent, 'stopPropagation');

    const dialogRefMock = { afterClosed: () => of(false) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDelete(mockEvent);

    expect(mockEvent.stopPropagation).toHaveBeenCalled();
    expect(dialogMock.open).toHaveBeenCalled();
  });

  it('should delete product when confirmed', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(mockEvent, 'stopPropagation');
    spyOn(component.delete, 'emit');

    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDelete(mockEvent);

    expect(productServiceMock.deleteProduct).toHaveBeenCalledWith('prod-123');
    expect(component.delete.emit).toHaveBeenCalled();
  });

  it('should not delete product when cancelled', () => {
    const mockEvent = new MouseEvent('click');
    const dialogRefMock = { afterClosed: () => of(false) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDelete(mockEvent);

    expect(productServiceMock.deleteProduct).not.toHaveBeenCalled();
  });

  it('should handle delete error', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(console, 'error');
    productServiceMock.deleteProduct.and.returnValue(throwError(() => new Error('Delete failed')));

    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDelete(mockEvent);

    expect(console.error).toHaveBeenCalledWith('Failed to delete product', jasmine.any(Error));
  });

  it('should emit edit event', () => {
    spyOn(component.edit, 'emit');

    component.edit.emit();

    expect(component.edit.emit).toHaveBeenCalled();
  });

  it('should handle null product in onDelete', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(mockEvent, 'stopPropagation');
    component.product = null;

    component.onDelete(mockEvent);

    expect(mockEvent.stopPropagation).toHaveBeenCalled();
    expect(dialogMock.open).not.toHaveBeenCalled();
  });

  it('should fetch full product and open edit modal on onEdit', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(mockEvent, 'stopPropagation');
    const fullProduct = { ...mockProduct, sellerFirstName: 'John', sellerLastName: 'Doe' } as any;
    productServiceMock.getProductById = jasmine.createSpy().and.returnValue(of(fullProduct));

    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    spyOn(component.edit, 'emit');

    component.onEdit(mockEvent);

    expect(mockEvent.stopPropagation).toHaveBeenCalled();
    expect(productServiceMock.getProductById).toHaveBeenCalledWith('prod-123');
    expect(dialogMock.open).toHaveBeenCalled();
    expect(component.edit.emit).toHaveBeenCalled();
  });

  it('should not emit edit if modal cancelled', () => {
    const mockEvent = new MouseEvent('click');
    const fullProduct = { ...mockProduct, sellerFirstName: 'John' } as any;
    productServiceMock.getProductById = jasmine.createSpy().and.returnValue(of(fullProduct));

    const dialogRefMock = { afterClosed: () => of(false) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    spyOn(component.edit, 'emit');

    component.onEdit(mockEvent);

    expect(component.edit.emit).not.toHaveBeenCalled();
  });

  it('should handle error when fetching product for edit', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(console, 'error');
    spyOn(window, 'alert');
    productServiceMock.getProductById = jasmine.createSpy().and.returnValue(throwError(() => new Error('Fetch failed')));

    component.onEdit(mockEvent);

    expect(console.error).toHaveBeenCalled();
    expect(window.alert).toHaveBeenCalledWith('Could not open editor. Please try again.');
  });

  it('should not open edit modal if product is null', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(mockEvent, 'stopPropagation');
    component.product = null;

    component.onEdit(mockEvent);

    expect(mockEvent.stopPropagation).toHaveBeenCalled();
    expect(dialogMock.open).not.toHaveBeenCalled();
  });

  it('should log error on delete failure', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(console, 'error');
    productServiceMock.deleteProduct.and.returnValue(throwError(() => new Error('Delete failed')));

    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDelete(mockEvent);

    expect(console.error).toHaveBeenCalledWith('Failed to delete product', jasmine.any(Error));
  });

  it('should log success message on delete', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(console, 'log');
    productServiceMock.deleteProduct.and.returnValue(of('Product deleted successfully'));

    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDelete(mockEvent);

    expect(console.log).toHaveBeenCalledWith('Product deleted successfully');
  });

  it('should not call navigate if product is null on card click', () => {
    component.product = null;

    component.onCardClick();

    expect(routerMock.navigate).not.toHaveBeenCalled();
  });

  it('should start carousel with multiple images', fakeAsync(() => {
    component.product = { ...mockProduct, imageUrls: ['/img1.jpg', '/img2.jpg', '/img3.jpg'] };
    component.currentImageIndex = 0;

    component.ngOnInit();
    tick(3000);

    expect(component.currentImageIndex).toBe(1);

    tick(3000);
    expect(component.currentImageIndex).toBe(2);

    tick(3000);
    expect(component.currentImageIndex).toBe(0); // Wraps around

    component.ngOnDestroy();
  }));

  it('should not start carousel with single image', fakeAsync(() => {
    component.product = { ...mockProduct, imageUrls: ['/img1.jpg'] };
    component.currentImageIndex = 0;

    component.ngOnInit();
    tick(3000);

    expect(component.currentImageIndex).toBe(0); // Doesn't change

    component.ngOnDestroy();
  }));

  it('should pause carousel on mouse enter', fakeAsync(() => {
    component.product = { ...mockProduct, imageUrls: ['/img1.jpg', '/img2.jpg'] };
    component.currentImageIndex = 0;

    component.ngOnInit();
    tick(1500); // Half way through interval

    component.onMouseEnter(); // Pause

    tick(3000); // Wait longer than interval
    expect(component.currentImageIndex).toBe(0); // Hasn't advanced

    component.ngOnDestroy();
  }));

  it('should restart carousel on mouse leave', fakeAsync(() => {
    component.product = { ...mockProduct, imageUrls: ['/img1.jpg', '/img2.jpg'] };
    component.currentImageIndex = 0;

    component.onMouseEnter(); // Pause (no carousel running)
    component.onMouseLeave(); // Restart

    tick(3000);
    expect(component.currentImageIndex).toBe(1);

    component.ngOnDestroy();
  }));

  it('should clear interval on destroy', () => {
    spyOn(window, 'clearInterval');
    component.imageChangeInterval = 123;

    component.ngOnDestroy();

    expect(clearInterval).toHaveBeenCalledWith(123);
  });

  it('should not clear interval if none exists', () => {
    component.imageChangeInterval = null;

    expect(() => component.ngOnDestroy()).not.toThrow();
  });
});