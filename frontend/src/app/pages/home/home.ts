import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // For *ngIf
import { FormsModule } from '@angular/forms'; // For [(ngModel)]
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
// Import our new components and services
import { ProductCard } from '../../components/product-card/product-card';
import { ProductService, Page } from '../../services/product-service'; // Adjust path
import { ProductCardDTO } from '../../models/productCard.model'; // Adjust path
import { AuthService } from '../../services/auth';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatPaginatorModule,
    ProductCard // Import the reusable card component
  ],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  // User data state
  currentUser: User | null = null;
  errorMessage: string | null = null;
  filterError: string | null = null;

  // Product data state
  products: ProductCardDTO[] = [];

  // Paginator properties
  totalElements: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;

  // Search/Filter properties
  searchKeyword: string = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  minQuantity: number | null = null;
  maxQuantity: number | null = null;
  startDate: string = '';
  endDate: string = '';

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
        console.error('Failed to fetch current user:', err);
        this.errorMessage = 'Could not load user data.';
      }
    });

    // 2. Fetch the first page of products
    this.fetchProducts();
  }

  fetchProducts(): void {
    // Validate filters before hitting the API
    if (!this.isFilterValid()) {
      return;
    }

    // Check if any filters are active (use trim() to ignore empty strings)
    const hasFilters = (this.searchKeyword && this.searchKeyword.trim()) ||
      this.minPrice != null ||
      this.maxPrice != null ||
      this.minQuantity != null ||
      this.maxQuantity != null ||
      this.startDate ||
      this.endDate;

    if (hasFilters) {
      // Use search endpoint when filters are applied
      this.productService.searchProducts(
        this.searchKeyword || undefined,
        this.minPrice || undefined,
        this.maxPrice || undefined,
        this.minQuantity || undefined,
        this.maxQuantity || undefined,
        this.startDate || undefined,
        this.endDate || undefined,
        this.pageIndex,
        this.pageSize
      ).subscribe((page: Page<ProductCardDTO>) => {
        this.products = page.content;
        this.totalElements = page.totalElements;
      });
    } else {
      // Use getAllProducts endpoint when no filters
      this.productService.getAllProducts(this.pageIndex, this.pageSize)
        .subscribe((page: Page<ProductCardDTO>) => {
          this.products = page.content;
          this.totalElements = page.totalElements;
        });
    }
    console.log("image urls", this.products);
  }

  // This is called by the <mat-paginator>
  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.fetchProducts();
  }

  // Search and filter handler
  onSearch(): void {
    this.pageIndex = 0; // Reset to first page
    this.fetchProducts();
  }

  // Clear all filters
  clearFilters(): void {
    this.searchKeyword = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.minQuantity = null;
    this.maxQuantity = null;
    this.startDate = '';
    this.endDate = '';
    this.filterError = null;
    this.pageIndex = 0;
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

  /**
   * Frontend validation to prevent invalid filter submissions
   */
  private isFilterValid(): boolean {
    this.filterError = null;

    // Non-negative checks
    if ((this.minPrice ?? 0) < 0 || (this.maxPrice ?? 0) < 0) {
      this.filterError = 'Price cannot be negative.';
      return false;
    }
    if ((this.minQuantity ?? 0) < 0 || (this.maxQuantity ?? 0) < 0) {
      this.filterError = 'Quantity cannot be negative.';
      return false;
    }

    // Min/Max ordering checks
    if (this.minPrice != null && this.maxPrice != null && this.minPrice > this.maxPrice) {
      this.filterError = 'Min price must be less than or equal to max price.';
      return false;
    }
    if (this.minQuantity != null && this.maxQuantity != null && this.minQuantity > this.maxQuantity) {
      this.filterError = 'Min quantity must be less than or equal to max quantity.';
      return false;
    }

    // Date ordering check
    if (this.startDate && this.endDate) {
      const start = new Date(this.startDate);
      const end = new Date(this.endDate);
      if (start > end) {
        this.filterError = 'Start date must be before end date.';
        return false;
      }
    }

    return true;
  }
}
