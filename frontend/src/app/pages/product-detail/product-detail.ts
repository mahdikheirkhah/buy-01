import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ConfirmDialog } from '../../components/confirm-dialog/confirm-dialog';
import { EditProductModal } from '../../components/edit-product-modal/edit-product-modal';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  template: `
    <div class="product-detail-container">
      <div *ngIf="isLoading" class="spinner-container">
        <mat-spinner></mat-spinner>
      </div>

      <div *ngIf="errorMessage" class="error-container">
        <p>{{ errorMessage }}</p>
        <a routerLink="/home">Go back home</a>
      </div>

      <div *ngIf="product" class="product-content-grid">
        <div class="product-gallery">
          <div class="main-image-container">
            <img *ngIf="selectedImageUrl" [src]="selectedImageUrl" alt="{{ product.name }}">
            <div *ngIf="!selectedImageUrl" class="no-image-placeholder">
              <mat-icon>image_not_supported</mat-icon>
            </div>
          </div>

          <div class="thumbnail-list" *ngIf="product.media.length > 1">
            <img *ngFor="let media of product.media"
                 [src]="getFullImageUrl(media.fileUrl)"
                 (click)="selectImage(media.fileUrl)"
                 [class.active]="getFullImageUrl(media.fileUrl) === selectedImageUrl"
                 alt="Thumbnail">
          </div>
        </div>

        <div class="product-info">
          <h1>{{ product.name }}</h1>
          <div class="price">{{ product.price | currency:'USD' }}</div>
          <div class="quantity">
            <strong>Quantity Available:</strong> {{ product.quantity }}
          </div>
          <div class="description">
            <h3>Description</h3>
            <p>{{ product.description }}</p>
          </div>
          <div class="seller-info">
            <strong>Sold by:</strong> {{ product.sellerFirstName }} {{ product.sellerLastName }}
            <span>({{ product.sellerEmail }})</span>
          </div>
          <div class="action-buttons" *ngIf="product.createdByMe">
            <button mat-raised-button color="primary" (click)="onEdit()">
              <mat-icon>edit</mat-icon>
              Edit Product
            </button>
            <button mat-raised-button color="warn" (click)="onDelete()">
              <mat-icon>delete</mat-icon>
              Delete Product
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .product-detail-container {
      max-width: 1200px;
      margin: 80px auto 40px;
      padding: 24px;
      background-color: var(--white);
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      min-height: 60vh;
    }

    .spinner-container, .error-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px;
    }

    .error-container p {
      font-size: 1.2rem;
      color: #d32f2f;
    }

    .product-content-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 32px;
    }

    .product-gallery {
      display: flex;
      flex-direction: column;
    }

    .main-image-container {
      width: 100%;
      aspect-ratio: 1 / 1;
      border: 1px solid #eee;
      border-radius: 8px;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f9f9f9;
    }

    .main-image-container img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

    .thumbnail-list {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: 16px;
    }

    .thumbnail-list img {
      width: 80px;
      height: 80px;
      object-fit: cover;
      border: 2px solid #ddd;
      border-radius: 4px;
      cursor: pointer;
      transition: border-color 0.2s;
    }

    .thumbnail-list img:hover {
      border-color: #aaa;
    }

    .thumbnail-list img.active {
      border-color: var(--navy);
    }

    .product-info h1 {
      margin-top: 0;
      color: var(--navy);
      font-size: 2.5rem;
    }

    .product-info .price {
      font-size: 2rem;
      font-weight: 300;
      color: var(--green);
      margin-bottom: 20px;
    }

    .product-info .quantity,
    .product-info .seller-info {
      font-size: 1rem;
      color: #555;
      margin-bottom: 16px;
    }

    .product-info .seller-info span {
      margin-left: 8px;
      color: #777;
    }

    .product-info .description {
      margin-top: 24px;
    }

    .product-info .description p {
      line-height: 1.6;
    }

    .action-buttons {
      margin-top: 30px;
      display: flex;
      gap: 16px;
    }

    @media (max-width: 768px) {
      .product-content-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ProductDetail implements OnInit {
  product: ProductDetailDTO | null = null;
  selectedImageUrl: string | null = null;
  isLoading = true;
  errorMessage: string | null = null;


  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    public dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.authService.fetchCurrentUser().subscribe();
    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.productService.getProductById(id).subscribe({
        next: (data) => {
          this.product = data;
          console.log(this.product);
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
    if (!this.product) return;

    const dialogRef = this.dialog.open(EditProductModal, {
      width: '600px',
      data: { product: this.product } // We already have the full product data
    });

    // After the modal closes, refresh this page's data
    dialogRef.afterClosed().subscribe(wasSuccessful => {
      if (wasSuccessful) {
        this.ngOnInit();
      }
    });
  }

  onDelete(): void {
    if (!this.product) return;

    const dialogRef = this.dialog.open(ConfirmDialog, {
      width: '350px',
      data: {
        title: 'Delete Product',
        message: `Are you sure you want to delete "${this.product.name}"? This action cannot be undone.`
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true && this.product) {
        this.productService.deleteProduct(this.product.productId).subscribe({
          next: (response) => {
            console.log(response);
            this.router.navigate(['/my-products']);
          },
          error: (err) => {
            console.error('Failed to delete product', err);
            // TODO: Show a snackbar or alert
          }
        });
      }
    });
  }
}
