import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { ProductDetailDTO,MediaUploadResponseDTO } from '../../models/product.model';
import { ProductService } from '../../services/product-service';
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
  currentMedia: MediaUploadResponseDTO[] = []; // Local copy of media
  filesToUpload: File[] = [];
  fileErrors: string[] = [];
  isLoading = false;


  private maxSizeInBytes = 2 * 1024 * 1024; // 2MB
  private allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    public dialogRef: MatDialogRef<EditProductModal>,
    @Inject(MAT_DIALOG_DATA) public data: { product: ProductDetailDTO }
  ) {
    this.product = data.product;
    // Create a deep copy of the media array to safely modify it
    this.currentMedia = (JSON.parse(JSON.stringify(this.product.media || [])) as MediaUploadResponseDTO[])
      .map(mediaItem => ({
        ...mediaItem, // <-- Copies all properties from the original media item
        productId: this.product.productId // <-- Adds the new property
      }));

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
    for (const file of files) {
      if (!this.allowedTypes.includes(file.type)) {
        this.fileErrors.push(`Invalid type: ${file.name}`);
        continue;
      }
      if (file.size > this.maxSizeInBytes) {
        this.fileErrors.push(`File too large: ${file.name} (Max 2MB)`);
        continue;
      }
      this.filesToUpload.push(file);
    }
  }

  removeNewFile(index: number): void {
    this.filesToUpload.splice(index, 1);
  }

  deleteExistingImage(productId: string, mediaId: string, index: number): void {
    // 1. Call the service to delete from backend
    this.productService.deleteProductImage(productId, mediaId).subscribe({
      next: () => {
        // 2. On success, remove from the local UI list
        this.currentMedia.splice(index, 1);
      },
      error: (err) => {
        console.error('Failed to delete image', err);
        alert('Could not delete image. Please try again.');
      }
    });
  }

  // --- Form Actions ---

  onSave(): void {
    if (this.editForm.invalid) return;
    this.isLoading = true;

    // Create a list of all tasks to run
    const tasks: Observable<any>[] = [];

    // Task 1: Update Product Details (if changed)
    if (this.editForm.dirty) {
      tasks.push(this.productService.updateProduct(this.product.productId, this.editForm.value));
    }

    // Tasks 2: Upload New Images
    const uploadTasks = this.filesToUpload.map(file =>
      this.productService.uploadProductImage(this.product.productId, file)
    );
    tasks.push(...uploadTasks);

    // Run all tasks in parallel
    forkJoin(tasks).subscribe({
      next: () => {
        this.isLoading = false;
        this.dialogRef.close(true); // Close and signal success (true)
      },
      error: (err) => {
        this.isLoading = false;
        alert('An error occurred while saving. Please try again.');
        console.error(err);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(false); // Close and signal no change
  }

  getImageUrl(imagePath: string): string {
    return `https://localhost:8443${imagePath}`;
  }
}
