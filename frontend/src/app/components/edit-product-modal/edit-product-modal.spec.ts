import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { EditProductModal } from './edit-product-modal';
import { ProductService } from '../../services/product-service';

describe('EditProductModal', () => {
  let component: EditProductModal;

  beforeEach(async () => {
    const productServiceMock = jasmine.createSpyObj('ProductService', ['updateProduct']);

    const mockProduct = {
      productId: 'test-id',
      name: 'Test Product',
      description: 'Test Description',
      price: 99.99,
      quantity: 10,
      sellerFirstName: 'John',
      sellerLastName: 'Doe',
      sellerEmail: 'john@example.com',
      createdByMe: true,
      media: []
    };

    await TestBed.configureTestingModule({
      imports: [EditProductModal, HttpClientTestingModule],
      providers: [
        { provide: ProductService, useValue: productServiceMock },
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: { product: mockProduct } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(EditProductModal);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
