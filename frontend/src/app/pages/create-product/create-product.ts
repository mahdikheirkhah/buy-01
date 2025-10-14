import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms'; // ✅ Import ReactiveFormsModule
import { Router, RouterLink } from '@angular/router'; // ✅ Import RouterLink
import { CommonModule } from '@angular/common'; // ✅ Import CommonModule
import { ProductService } from '../../services/product-service';

// ✅ Import all the Angular Material modules you are using
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';


@Component({
  selector: 'app-create-product',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './create-product.html',
  styleUrls: ['./create-product.css']
})
export class CreateProduct {
  productForm: FormGroup;
  selectedFiles: File[] = [];

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private router: Router
  ) {
    this.productForm = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      price: [0, [Validators.required, Validators.min(0)]],
      quantity: [0, [Validators.required, Validators.min(0)]],
    });
  }

  onFileSelected(event: any): void {
    this.selectedFiles = Array.from(event.target.files);
  }

  async onSubmit(): Promise<void> {
    if (this.productForm.invalid) {
      return;
    }

    try {
      // Step 1: Create the product with text data
      const productResponse: any = await this.productService.createProduct(this.productForm.value).toPromise();
      const productId = productResponse.id;

      // Step 2: Upload images one by one
      if (this.selectedFiles.length > 0) {
        for (const file of this.selectedFiles) {
          await this.productService.uploadProductImage(productId, file).toPromise();
        }
      }

      alert('Product created successfully!');
      this.router.navigate(['/my-products']); // Navigate to another page on success

    } catch (error) {
      console.error('Failed to create product:', error);
      alert('Error creating product. Please try again.');
    }
  }
}
