import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // For *ngIf
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
    MatPaginatorModule,
    ProductCard
  ],
  template: `
    <div class="home-container">
      <div *ngIf="currentUser" class="welcome-banner">
        <h2>Welcome back, {{ currentUser.firstName }}!</h2>
        <p>Check out the latest products from all our sellers.</p>
      </div>
      <div *ngIf="errorMessage" class="error-banner">
        <p>{{ errorMessage }}</p>
      </div>

      <div class="product-grid" *ngIf="products.length > 0; else noProducts">
        <app-product-card
          *ngFor="let product of products"
          [product]="product"
          (edit)="onProductUpdated()"
          (delete)="onProductDeleted()">
        </app-product-card>
      </div>

      <ng-template #noProducts>
        <p>No products have been listed yet. Check back soon!</p>
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
    .home-container {
      max-width: 1200px;
      margin: 80px auto 40px;
      padding: 24px;
    }

    .welcome-banner {
      background-color: var(--white);
      border-radius: 8px;
      padding: 24px;
      margin-bottom: 30px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }

    .welcome-banner h2 {
      margin-top: 0;
      color: var(--navy);
    }

    .error-banner {
      background-color: #fff0f0;
      color: #c51111;
      border: 1px solid #fcc;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 30px;
    }

    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
    }

    mat-paginator {
      margin-top: 24px;
      background-color: var(--background-light);
    }
  `]
})
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
        console.error('Failed to fetch current user:', err);
        this.errorMessage = 'Could not load user data.';
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
