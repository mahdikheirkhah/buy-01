import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.css']
})
export class ProductDetail implements OnInit {
  product: ProductDetailDTO | null = null;
  selectedImageUrl: string | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productService.getProductById(id).subscribe({
        next: (data) => {
          this.product = data;
          // Set the first image as the default selected one
          if (data.media && data.media.length > 0) {
            this.selectedImageUrl = this.getFullImageUrl(data.media[0].fileUrl);
          }
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Failed to fetch product', err);
          this.errorMessage = 'Could not load product details.';
          this.isLoading = false;
        }
      });
    }
  }

  // Helper to change the main displayed image
  selectImage(fileUrl: string): void {
    this.selectedImageUrl = this.getFullImageUrl(fileUrl);
  }

  // Helper to build the full URL
  getFullImageUrl(path: string): string {
    return `https://localhost:8443${path}`;
  }

  onEdit(): void {
    // Navigate to an edit page (e.g., /product/edit/123)
    if (this.product) {
      this.router.navigate(['/product/edit', this.product.productId]);
    }
  }

  onDelete(): void {
    if (this.product) {
      // Implement delete confirmation logic
      console.log('Deleting product:', this.product.productId);
    }
  }
}
