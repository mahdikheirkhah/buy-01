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
  template: `
    <h2 mat-dialog-title>Edit Product</h2>

    <div *ngIf="isLoading" [class.loading]="isLoading" class="loading-overlay">
      <mat-spinner></mat-spinner>
    </div>

    <div mat-dialog-content [class.loading]="isLoading">
      <mat-tab-group>
        <mat-tab label="Details">
          <form [formGroup]="editForm" class="details-form">
            <mat-form-field appearance="fill">
              <mat-label>Product Name</mat-label>
              <input matInput formControlName="name">
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Description</mat-label>
              <textarea matInput formControlName="description"></textarea>
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Price</mat-label>
              <input matInput type="number" formControlName="price">
            </mat-form-field>

            <mat-form-field appearance="fill">
              <mat-label>Quantity</mat-label>
              <input matInput type="number" formControlName="quantity">
            </mat-form-field>
          </form>
        </mat-tab>

        <mat-tab label="Manage Images">
          <div class="image-section">
            <h4>Current Images</h4>
            <p *ngIf="currentMedia.length === 0">This product has no images.</p>
            <div class="image-grid">
              <div *ngFor="let media of currentMedia; let i = index"
                   class="thumbnail-wrapper"
                   [class.deleting]="mediaToDelete.includes(media.fileId)">
                <img [src]="getImageUrl(media.fileUrl)" alt="Product Image" class="thumbnail">
                <button mat-icon-button class="delete-image-btn" (click)="deleteExistingImage(media.fileId, i)">
                  <mat-icon>cancel</mat-icon>
                </button>
              </div>
            </div>

            <mat-divider></mat-divider>

            <h4>Add New Images</h4>
            <div class="form-group">
              <input
                type="file"
                id="images"
                (change)="onFilesSelected($event)"
                accept="image/png, image/jpeg, image/gif, image/webp"
                multiple>
            </div>

            <div class="file-errors" *ngIf="fileErrors.length > 0">
              <p *ngFor="let error of fileErrors">{{ error }}</p>
            </div>

            <div class="file-list" *ngIf="filesToUpload.length > 0">
              <p>Staged for upload:</p>
              <ul>
                <li *ngFor="let file of filesToUpload; let i = index">
                  <span>{{ file.name }}</span>
                  <button type="button" (click)="removeNewFile(i)" class="remove-btn">
                    <mat-icon>cancel</mat-icon>
                  </button>
                </li>
              </ul>
            </div>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>

    <div mat-dialog-actions align="end" [class.loading]="isLoading">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-flat-button color="primary" (click)="onSave()"
              [disabled]="editForm.invalid || !hasChanges">
        Save Changes
      </button>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      position: relative;
    }

    .loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(255, 255, 255, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10;
    }

    .loading {
      filter: blur(2px);
      pointer-events: none;
    }

    .details-form {
      padding-top: 20px;
      display: flex;
      flex-direction: column;
    }

    form mat-form-field {
      width: 100%;
    }

    mat-divider {
      margin: 20px 0;
    }

    .image-section h4 {
      color: var(--navy);
      margin-top: 0;
    }

    .image-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: 10px;
      margin-bottom: 20px;
    }

    .thumbnail-wrapper {
      position: relative;
      width: 100px;
      height: 100px;
      border: 1px solid #ddd;
      border-radius: 8px;
      overflow: hidden;
      background-color: #f0f0f0;
    }

    .thumbnail {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

    .delete-image-btn {
      position: absolute;
      top: 0px;
      right: 0px;
      background-color: rgba(0, 0, 0, 0.6);
      color: white;
      width: 24px;
      height: 24px;
      line-height: 24px;
      border: none;
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      z-index: 10;
      border-radius: 0 8px 0 8px;
    }

    .delete-image-btn:hover {
      background-color: rgba(244, 67, 54, 0.9);
    }

    .delete-image-btn mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      line-height: 18px;
    }

    .thumbnail-wrapper:hover {
      animation: shake 0.5s;
      animation-iteration-count: infinite;
    }

    @keyframes shake {
      0% { transform: translate(0, 0) rotate(0deg); }
      25% { transform: translate(1px, 1px) rotate(1deg); }
      50% { transform: translate(-1px, -1px) rotate(-1deg); }
      75% { transform: translate(1px, -1px) rotate(1deg); }
      100% { transform: translate(0, 0) rotate(0deg); }
    }

    .form-group {
      margin-top: 20px;
    }

    .file-list {
      margin-top: 15px;
      background-color: #f9f9f9;
      border: 1px dashed #ccc;
      border-radius: 4px;
      padding: 10px;
    }

    .file-list p {
      margin-top: 0;
      font-weight: 500;
    }

    .file-list ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .file-list li {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px;
    }

    .remove-btn {
      background: none;
      border: none;
      cursor: pointer;
    }

    .remove-btn mat-icon {
      color: #f44336;
    }

    .file-errors {
      padding: 10px;
      background-color: #fff0f0;
      color: #c51111;
      border: 1px solid #fcc;
      border-radius: 4px;
    }
  `]
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

  ngOnInit(): void { }

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

