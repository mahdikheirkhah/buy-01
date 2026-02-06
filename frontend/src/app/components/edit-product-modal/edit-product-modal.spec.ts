import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { EditProductModal } from './edit-product-modal';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';

xdescribe('EditProductModal', () => {
  let component: EditProductModal;
  let fixture: ComponentFixture<EditProductModal>;
  let productServiceMock: jasmine.SpyObj<ProductService>;
  let dialogRefMock: jasmine.SpyObj<MatDialogRef<EditProductModal>>;

  const mockProduct: ProductDetailDTO = {
    productId: 'test-id',
    name: 'Test Product',
    description: 'Test Description',
    price: 99.99,
    quantity: 10,
    sellerId: 'seller-123',
    sellerFirstName: 'John',
    sellerLastName: 'Doe',
    sellerEmail: 'john@example.com',
    createdByMe: true,
    media: [
      { fileId: 'media-1', fileUrl: '/uploads/image1.jpg', productId: 'test-id' },
      { fileId: 'media-2', fileUrl: '/uploads/image2.jpg', productId: 'test-id' }
    ]
  };

  beforeEach(async () => {
    productServiceMock = jasmine.createSpyObj('ProductService', [
      'updateProduct',
      'uploadProductImage',
      'deleteProductImage'
    ]);
    productServiceMock.updateProduct.and.returnValue(of(mockProduct));
    productServiceMock.uploadProductImage.and.returnValue(of({ fileId: 'new-media', fileUrl: '/uploads/new.jpg' }));
    productServiceMock.deleteProductImage.and.returnValue(of({ message: 'Image deleted' }));

    dialogRefMock = jasmine.createSpyObj('MatDialogRef', ['close']);

    // Override component to use inline template
    TestBed.overrideComponent(EditProductModal, {
      set: {
        template: `
          <form [formGroup]="editForm">
            <input formControlName="name" />
            <input formControlName="description" />
            <input formControlName="price" />
            <input formControlName="quantity" />
            <button (click)="onSave()">Save</button>
            <button (click)="onCancel()">Cancel</button>
          </form>
        `,
        styles: []
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        EditProductModal,
        HttpClientTestingModule,
        ReactiveFormsModule,
        CommonModule,
        CurrencyPipe,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatDividerModule,
        MatTabsModule
      ],
      providers: [
        { provide: ProductService, useValue: productServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: { product: mockProduct } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EditProductModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with product data', () => {
    expect(component.editForm.value).toEqual({
      name: 'Test Product',
      description: 'Test Description',
      price: 99.99,
      quantity: 10
    });
  });

  it('should copy media array to currentMedia', () => {
    expect(component.currentMedia.length).toBe(2);
    expect(component.currentMedia[0].fileId).toBe('media-1');
  });

  it('should validate required fields', () => {
    component.editForm.patchValue({ name: '', description: '', price: null, quantity: null });

    expect(component.editForm.valid).toBe(false);
    expect(component.editForm.get('name')?.errors?.['required']).toBe(true);
    expect(component.editForm.get('description')?.errors?.['required']).toBe(true);
  });

  it('should validate minimum price', () => {
    component.editForm.patchValue({ price: 0 });

    expect(component.editForm.get('price')?.errors?.['min']).toBe(true);
  });

  it('should validate minimum quantity', () => {
    component.editForm.patchValue({ quantity: -1 });

    expect(component.editForm.get('quantity')?.errors?.['min']).toBe(true);
  });

  it('should add valid files to upload queue', () => {
    const file = new File(['image content'], 'test.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [file] } };

    component.onFilesSelected(event);

    expect(component.filesToUpload.length).toBe(1);
    expect(component.fileErrors.length).toBe(0);
  });

  it('should reject files with invalid type', () => {
    const file = new File(['content'], 'test.txt', { type: 'text/plain' });
    const event = { target: { files: [file] } };

    component.onFilesSelected(event);

    expect(component.filesToUpload.length).toBe(0);
    expect(component.fileErrors.length).toBe(1);
    expect(component.fileErrors[0]).toContain('invalid type');
  });

  it('should reject files larger than 2MB', () => {
    const largeFile = new File([new ArrayBuffer(3 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [largeFile] } };

    component.onFilesSelected(event);

    expect(component.filesToUpload.length).toBe(0);
    expect(component.fileErrors.length).toBe(1);
    expect(component.fileErrors[0]).toContain('too large');
  });

  it('should remove file from upload queue', () => {
    component.filesToUpload = [
      new File(['1'], 'test1.jpg', { type: 'image/jpeg' }),
      new File(['2'], 'test2.jpg', { type: 'image/jpeg' })
    ];

    component.removeNewFile(0);

    expect(component.filesToUpload.length).toBe(1);
    expect(component.filesToUpload[0].name).toBe('test2.jpg');
  });

  it('should mark media for deletion', () => {
    component.deleteExistingImage('media-1', 0);

    expect(component.mediaToDelete).toContain('media-1');
    expect(component.currentMedia.length).toBe(1);
  });

  it('should call onCancel to close dialog', () => {
    component.onCancel();

    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it('should not save with invalid form', () => {
    component.editForm.patchValue({ name: '' });

    component.onSave();

    expect(productServiceMock.updateProduct).not.toHaveBeenCalled();
  });

  it('should update product on save with dirty form', () => {
    component.editForm.markAsDirty();
    component.editForm.patchValue({ name: 'Updated Product' });

    component.onSave();

    expect(component.isLoading).toBe(true);
    expect(productServiceMock.updateProduct).toHaveBeenCalledWith('test-id', jasmine.objectContaining({
      name: 'Updated Product'
    }));
  });

  it('should upload new files on save', () => {
    const file = new File(['content'], 'new.jpg', { type: 'image/jpeg' });
    component.filesToUpload = [file];

    component.onSave();

    expect(productServiceMock.uploadProductImage).toHaveBeenCalledWith('test-id', file);
  });

  it('should delete marked images on save', () => {
    component.mediaToDelete = ['media-1'];

    component.onSave();

    expect(productServiceMock.deleteProductImage).toHaveBeenCalledWith('test-id', 'media-1');
  });

  it('should close dialog with true on successful save', () => {
    component.editForm.markAsDirty();

    component.onSave();

    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it('should handle save error', () => {
    spyOn(console, 'error');
    spyOn(window, 'alert');
    productServiceMock.updateProduct.and.returnValue(throwError(() => new Error('Update failed')));
    component.editForm.markAsDirty();

    component.onSave();

    expect(console.error).toHaveBeenCalled();
    expect(window.alert).toHaveBeenCalledWith('An error occurred while saving. Please try again.');
    expect(component.isLoading).toBe(false);
  });

  it('should close with false if no changes on save', () => {
    component.onSave();

    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
    expect(productServiceMock.updateProduct).not.toHaveBeenCalled();
  });

  it('should return true for hasChanges when form is dirty', () => {
    component.editForm.markAsDirty();

    expect(component.hasChanges).toBe(true);
  });

  it('should return true for hasChanges when files to upload', () => {
    component.filesToUpload = [new File(['test'], 'test.jpg', { type: 'image/jpeg' })];

    expect(component.hasChanges).toBe(true);
  });

  it('should return true for hasChanges when media to delete', () => {
    component.mediaToDelete = ['media-1'];

    expect(component.hasChanges).toBe(true);
  });

  it('should return false for hasChanges when no changes', () => {
    expect(component.hasChanges).toBe(false);
  });

  it('should clear changes on cancel', () => {
    component.mediaToDelete = ['media-1'];
    component.filesToUpload = [new File(['test'], 'test.jpg', { type: 'image/jpeg' })];

    component.onCancel();

    expect(component.mediaToDelete.length).toBe(0);
    expect(component.filesToUpload.length).toBe(0);
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it('should get full image URL', () => {
    const url = component.getImageUrl('/uploads/test.jpg');

    expect(url).toBe('https://localhost:8443/uploads/test.jpg');
  });
});
