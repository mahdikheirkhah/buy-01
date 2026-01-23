// my-products.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ProductService } from '../../services/product-service'; // Adjust path
import { ProductCardDTO } from '../../models/productCard.model'; // You'll need to create this model
import { ProductCard } from '../../components/product-card/product-card';
import { AuthService } from '../../services/auth';

// Create a model for the Page object
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // Current page number
}

@Component({
  selector: 'app-my-products',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatPaginatorModule, ProductCard],
  template: `
    <div class="product-page-container">
      <h2>My Products</h2>

      <div class="product-grid" *ngIf="products.length > 0; else noProducts">
        <app-product-card
          *ngFor="let product of products"
          [product]="product"
          (edit)="onProductUpdated()"
          (delete)="onProductDeleted()">
        </app-product-card>
      </div>

      <ng-template #noProducts>
        <p>You haven't created any products yet.</p>
      </ng-template>

      <mat-paginator
        [length]="totalElements"
        [pageSize]="pageSize"
        [pageSizeOptions]="[5, 10, 20]"
        (page)="onPageChange($event)">
      </mat-paginator>
    </div>
  `,
  styles: [`
    .product-page-container {
      max-width: 1200px;
      margin: 80px auto 40px;
      padding: 24px;
    }

    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
    }

    .product-card {
      display: flex;
      flex-direction: column;
    }

    .card-image-container {
      width: 100%;
      height: 200px;
      overflow: hidden;
    }

    .card-image-container img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .product-description {
      max-height: 100px;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 4;
      -webkit-box-orient: vertical;
    }

    mat-card-actions {
      margin-top: auto;
      padding: 8px 16px !important;
    }

    mat-paginator {
      margin-top: 24px;
      background-color: var(--background-light);
    }
  `]
})
export class MyProducts implements OnInit {
  products: ProductCardDTO[] = [];

  // Paginator properties
  totalElements: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;

  constructor(private productService: ProductService, private authService: AuthService) { }

  ngOnInit(): void {
    this.fetchMyProducts();
    this.authService.fetchCurrentUser().subscribe();
  }

  fetchMyProducts(): void {
    this.productService.getMyProducts(this.pageIndex, this.pageSize).subscribe((page: Page<ProductCardDTO>) => {
      this.products = page.content;
      this.totalElements = page.totalElements;
    });
  }

  // This is called by the <mat-paginator>
  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.fetchMyProducts();
  }

  onEdit(productId: string): void {
    // Navigate to edit page
    console.log('Edit product:', productId);
  }

  onProductDeleted(): void {
    console.log('Product deleted, refreshing list...');
    this.fetchMyProducts();
  }

  // Helper to build the full URL for the image
  getImageUrl(imagePath: string): string {
    return `https://localhost:8443${imagePath}`;
  }
  onProductUpdated(): void {
    console.log('Product was updated, refreshing list...');
    this.fetchMyProducts();
  }
}
