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
  templateUrl: './create-product.html',
  styleUrls: ['./create-product.css']
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
