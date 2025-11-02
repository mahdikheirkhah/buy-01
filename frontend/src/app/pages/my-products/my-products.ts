// my-products.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ProductService } from '../../services/product-service'; // Adjust path
import { ProductCardDTO } from '../../models/productCard.model'; // You'll need to create this model
import { ProductCard} from '../../components/product-card/product-card';
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
  templateUrl: './my-products.html',
  styleUrls: ['./my-products.css']
})
export class MyProducts implements OnInit {
  products: ProductCardDTO[] = [];

  // Paginator properties
  totalElements: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;

  constructor(private productService: ProductService,  private authService: AuthService) {}

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
