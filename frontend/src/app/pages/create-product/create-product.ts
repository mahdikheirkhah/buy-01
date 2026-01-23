// src/app/pages/create-product/create-product.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ProductService } from '../../services/product-service';
import { forkJoin, Observable } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth';
@Component({
  selector: 'app-create-product',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="container">
      <h1>Create a New Product</h1>

      <form [formGroup]="productForm" (ngSubmit)="onSubmit()">
        <div *ngIf="isUploading" class="upload-spinner">
          <mat-spinner></mat-spinner>
          <p>Creating product and uploading images, please wait...</p>
        </div>

        <ng-container *ngIf="!isUploading">
          <mat-form-field appearance="fill">
            <mat-label>Product Name</mat-label>
            <input matInput formControlName="name" required>
          </mat-form-field>

          <mat-form-field appearance="fill">
            <mat-label>Description</mat-label>
            <textarea matInput formControlName="description" required></textarea>
          </mat-form-field>

          <mat-form-field appearance="fill">
            <mat-label>Price</mat-label>
            <input matInput type="number" formControlName="price" required>
          </mat-form-field>

          <mat-form-field appearance="fill">
            <mat-label>Quantity</mat-label>
            <input matInput type="number" formControlName="quantity" required>
          </mat-form-field>

          <div class="form-group">
            <label for="images">Product Images (Max 2MB each)</label>
            <input type="file"
                   id="images"
                   (change)="onFilesSelected($event)"
                   accept="image/png, image/jpeg, image/gif, image/webp"
                   multiple>
          </div>

          <div class="file-errors" *ngIf="fileErrors.length > 0">
            <p *ngFor="let error of fileErrors">{{ error }}</p>
          </div>

          <div class="file-list" *ngIf="selectedFiles.length > 0">
            <p>Selected Files:</p>
            <ul>
              <li *ngFor="let file of selectedFiles; let i = index">
                <span>{{ file.name }} ({{ (file.size / 1024 / 1024) | number:'1.1-2' }} MB)</span>
                <button type="button" (click)="removeFile(i)" class="remove-btn">
                  <mat-icon>cancel</mat-icon>
                </button>
              </li>
            </ul>
          </div>

          <button mat-raised-button color="primary" type="submit" [disabled]="!productForm.valid || selectedFiles.length === 0">
            Create Product
          </button>
        </ng-container>
      </form>
    </div>
  `,
  styles: [`
    .container {
      max-width: 700px;
      margin: 80px auto 40px;
      padding: 24px 32px;
      background-color: var(--white);
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }

    .container h1 {
      text-align: center;
      color: var(--navy);
      margin-top: 0;
      margin-bottom: 24px;
    }

    form {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    mat-form-field,
    .form-group,
    .file-errors,
    .file-list {
      width: 100%;
      max-width: 500px;
      margin-bottom: 12px;
      box-sizing: border-box;
    }

    .form-group label {
      display: block;
      margin-bottom: 8px;
      font-weight: 500;
      color: #555;
    }

    .form-group input[type="file"] {
      width: 100%;
      padding: 10px;
      border: 1px solid #ccc;
      border-radius: 4px;
      background-color: #f9f9f9;
      box-sizing: border-box;
    }

    .file-errors {
      padding: 10px;
      background-color: #fff0f0;
      border: 1px solid #f44336;
      border-radius: 4px;
    }

    .file-errors p {
      color: #d32f2f;
      margin: 5px 0;
      font-size: 0.9rem;
    }

    .file-list {
      padding: 10px;
      background-color: #f9f9f9;
      border: 1px dashed #ccc;
      border-radius: 4px;
    }

    .file-list p {
      margin-top: 0;
      font-weight: 500;
      color: var(--navy);
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
      border-bottom: 1px solid #eee;
    }

    .file-list li:last-child {
      border-bottom: none;
    }

    .file-list li span {
      font-size: 0.9rem;
      color: #333;
    }

    .file-list li .remove-btn {
      background: none;
      border: none;
      cursor: pointer;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .file-list li .remove-btn mat-icon {
      color: #f44336;
      font-size: 20px;
    }

    .upload-spinner {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px;
      text-align: center;
    }

    .upload-spinner p {
      margin-top: 16px;
      font-size: 1.1rem;
      color: var(--navy);
    }

    button[type="submit"] {
      width: 100%;
      max-width: 500px;
      background-color: var(--green);
      color: var(--white);
      padding: 12px 0;
      font-size: 1rem;
      font-weight: 600;
      margin-top: 16px;
    }

    button[type="submit"]:hover {
      background-color: var(--green-dark);
    }

    button[type="submit"]:disabled {
      background-color: #ccc;
      color: #777;
    }
  `]
})
export class CreateProduct {
  productForm: FormGroup;
  selectedFiles: File[] = [];
  isUploading = false;

  // --- ✅ New Properties for Validation ---
  fileErrors: string[] = []; // To hold error messages
  private maxSizeInBytes = 12 * 1024 * 1024; // 2MB
  private allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
  // ----------------------------------------

  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private productService: ProductService,
    private router: Router
  ) {
    this.productForm = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      price: [null, [Validators.required, Validators.min(0.01)]],
      quantity: [null, [Validators.required, Validators.min(0)]],
    });
  }
  ngOnInit(): void {
    // Guarantees the AuthService.subject is populated even after a refresh
    this.authService.fetchCurrentUser().subscribe({
      error: () => {
        // 401 → user not logged in → subject already cleared by the service
        // (nothing else to do – navbar will show Login/Register)
      }
    });
  }
  /**
   * ✅ This method now ADDS files and VALIDATES them.
   */
  onFilesSelected(event: any): void {
    this.fileErrors = []; // Clear errors on new selection
    const files = event.target.files;

    if (files && files.length > 0) {
      for (const file of files) {
        // 1. Check type
        if (!this.allowedTypes.includes(file.type)) {
          this.fileErrors.push(`File: "${file.name}" has an invalid type. Only PNG, JPG, GIF, WebP are allowed.`);
          continue; // Skip this file
        }

        // 2. Check size
        if (file.size > this.maxSizeInBytes) {
          this.fileErrors.push(`File: "${file.name}" is too large (Max 2MB).`);
          continue; // Skip this file
        }

        // 3. If all checks pass, add the file
        this.selectedFiles.push(file);
      }
    }
  }

  /**
   * Removes a file from the list.
   */
  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  //
  // The onSubmit method remains exactly the same as before.
  // It will correctly upload all files in the 'selectedFiles' array.
  //
  onSubmit(): void {
    if (this.productForm.invalid || this.isUploading) {
      return;
    }
    this.isUploading = true;

    // Step 1: Create the product
    this.productService.createProduct(this.productForm.value).subscribe({
      next: (productResponse: any) => {
        const newProductId = productResponse.id;

        // Step 2: Check for images
        if (this.selectedFiles.length > 0) {

          // Create upload observables
          const uploadObservables: Observable<any>[] = this.selectedFiles.map(file => {
            return this.productService.uploadProductImage(newProductId, file);
          });

          // Step 3: Run all uploads in parallel
          forkJoin(uploadObservables).subscribe({
            next: (uploadResponses) => {
              console.log('All images uploaded successfully', uploadResponses);
              this.isUploading = false;
              this.router.navigate(['/my-products']);
            },
            error: (err) => {
              console.error('One or more image uploads failed', err);
              this.isUploading = false;
              alert('Product created, but image upload failed.');
              this.router.navigate(['/my-products']);
            }
          });

        } else {
          // No files, just navigate
          console.log('Product created with no images.');
          this.isUploading = false;
          this.router.navigate(['/my-products']);
        }
      },
      error: (err) => {
        console.error('Failed to create product', err);
        this.isUploading = false;
        alert('Error creating product. Please try again.');
      }
    });
  }
}
