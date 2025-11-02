import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { ProductDetailDTO, MediaUploadResponseDTO } from '../../models/product.model';
import { ProductService } from '../../services/product-service'; // Check this path
import { forkJoin, Observable, of } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';


@Component({
  selector: 'app-edit-product-modal',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatProgressSpinnerModule,
    MatDividerModule, MatTabsModule
  ],
  templateUrl: './edit-product-modal.html',
  styleUrls: ['./edit-product-modal.css']
})
export class EditProductModal implements OnInit {
  editForm: FormGroup;
  product: ProductDetailDTO;

  // Image tracking
  currentMedia: MediaUploadResponseDTO[] = [];
  filesToUpload: File[] = [];
  fileErrors: string[] = [];
  isLoading = false;

  // ✅ NEW: This array tracks images marked for deletion
  mediaToDelete: string[] = [];

  private maxSizeInBytes = 2 * 1024 * 1024; // 2MB
  private allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    public dialogRef: MatDialogRef<EditProductModal>,
    @Inject(MAT_DIALOG_DATA) public data: { product: ProductDetailDTO }
  ) {
    this.product = data.product;
    this.currentMedia = JSON.parse(JSON.stringify(this.product.media || []));

    this.editForm = this.fb.group({
      name: [this.product.name, Validators.required],
      description: [this.product.description, Validators.required],
      price: [this.product.price, [Validators.required, Validators.min(0.01)]],
      quantity: [this.product.quantity, [Validators.required, Validators.min(0)]],
    });
  }

  ngOnInit(): void {}

  // --- Image Handling ---

  onFilesSelected(event: any): void {
    this.fileErrors = [];
    const files = event.target.files;

    if (files && files.length > 0) {
      for (const file of files) {
        if (!this.allowedTypes.includes(file.type)) {
          this.fileErrors.push(`File: "${file.name}" has an invalid type.`);
          continue;
        }
        if (file.size > this.maxSizeInBytes) {
          this.fileErrors.push(`File: "${file.name}" is too large (Max 2MB).`);
          continue;
        }
        this.filesToUpload.push(file);
      }
    }
  }

  removeNewFile(index: number): void {
    this.filesToUpload.splice(index, 1);
  }

deleteExistingImage(mediaId: string, index: number): void {
  if (!this.mediaToDelete.includes(mediaId)) {
    this.mediaToDelete.push(mediaId);
  }
  this.currentMedia.splice(index, 1);
}

  // --- Form Actions ---

  onSave(): void {
    if (this.editForm.invalid) return;
    this.isLoading = true;

    const tasks: Observable<any>[] = [];

    // Task 1: Update Product Details (if form is changed)
    if (this.editForm.dirty) {
      tasks.push(this.productService.updateProduct(this.product.productId, this.editForm.value));
    }

    // Tasks 2: Upload New Images (if any were added)
    const uploadTasks = this.filesToUpload.map(file =>
      this.productService.uploadProductImage(this.product.productId, file)
    );
    tasks.push(...uploadTasks);

    // ✅ NEW: Task 3: Delete any images the user marked for deletion
    const deleteTasks = this.mediaToDelete.map(mediaId =>
      this.productService.deleteProductImage(this.product.productId, mediaId)
    );
    tasks.push(...deleteTasks);

    // Check if there's nothing to do (no changes, no uploads, no deletes)
    if (tasks.length === 0) {
      this.isLoading = false;
      this.dialogRef.close(false); // Close, no changes
      return;
    }

    // Run all tasks in parallel
    forkJoin(tasks).subscribe({
      next: () => {
        this.isLoading = false;
        this.dialogRef.close(true); // Close and signal success
      },
      error: (err) => {
        this.isLoading = false;
        alert('An error occurred while saving. Please try again.');
        console.error(err);
      }
    });
  }

onCancel(): void {
  this.mediaToDelete = [];
  this.filesToUpload = [];
  this.dialogRef.close(false);
}
get hasChanges(): boolean {
  return this.editForm.dirty || this.filesToUpload.length > 0 || this.mediaToDelete.length > 0;
}

  getImageUrl(imagePath: string): string {
    return `https://localhost:8443${imagePath}`;
  }
}

