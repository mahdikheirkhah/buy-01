import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // For *ngIf
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
// Import our new components and services
import { ProductCard } from '../../components/product-card/product-card';
import { ProductService, Page } from '../../services/product-service'; // Adjust path
import { ProductCardDTO } from '../../models/productCard.model'; // Adjust path
import { AuthService } from '../../services/auth';
import { User } from '../../models/user.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    MatPaginatorModule,
    ProductCard
  ],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
// @Component({
//   selector: 'app-home',
//   standalone: true,
//   imports: [
//     CommonModule,
//     MatPaginatorModule,
//     ProductCard
//   ],
//   templateUrl: './home.html',
//   styleUrls: ['./home.css']
// })
export class HomeComponent implements OnInit {
  // User data state
  currentUser: User | null = null;
  errorMessage: string | null = null;

  // Product data state
  products: ProductCardDTO[] = [];

  // Paginator properties
  totalElements: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;

  constructor(
    private authService: AuthService,
    private productService: ProductService // Inject ProductService
  ) { }

  ngOnInit(): void {
    // 1. Fetch the user (like before)
    this.authService.fetchCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
      },
      error: (err) => {
        // 401 means JWT is invalid/expired - let the interceptor handle logout
        // Don't show error message - user is already being redirected
        if (err.status !== 401) {
          console.error('Failed to fetch current user:', err);
          this.errorMessage = 'Could not load user data.';
        }
      }
    });

    // 2. Fetch the first page of products
    this.fetchProducts();
  }

  fetchProducts(): void {
    this.productService.getAllProducts(this.pageIndex, this.pageSize).subscribe((page: Page<ProductCardDTO>) => {
      this.products = page.content;
      this.totalElements = page.totalElements;
    });
    console.log("image urls", this.products);
  }

  // This is called by the <mat-paginator>
  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.fetchProducts();
  }

  // The home page doesn't need edit/delete, so we can
  // create empty handlers or just not bind them.
  onEdit(productId: string): void {
    console.log('Edit (from home):', productId);
    // You could navigate to an edit page here
  }

  onProductDeleted(): void {
    console.log('Product deleted from home, refreshing list...');
    this.fetchProducts();
  }
  onProductUpdated(): void {
    console.log('Product was updated, refreshing list...');
    this.fetchProducts();
  }
}
