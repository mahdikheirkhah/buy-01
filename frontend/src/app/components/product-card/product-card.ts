// src/app/components/product-card/product-card.component.ts
import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ProductCardDTO } from '../../models/productCard.model'; // Adjust path
import { CurrencyPipe } from '@angular/common'; // For the price
import { Router, RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ConfirmDialog } from '../confirm-dialog/confirm-dialog';
import { ProductService } from '../../services/product-service';
import { EditProductModal } from '../edit-product-modal/edit-product-modal';
import { ProductDetailDTO, MediaUploadResponseDTO } from '../../models/product.model';
@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    CurrencyPipe,
    MatDialogModule,
    EditProductModal
  ],
  template: `
    <mat-card *ngIf="product" class="product-card" (click)="onCardClick()">
      <div class="card-image-container">
        <img
          *ngFor="let imageUrl of product.imageUrls; let i = index"
          mat-card-image
          [src]="getImageUrl(imageUrl)"
          [class.active]="i === currentImageIndex" alt="{{ product.name }} (image {{i + 1}})">
        <div *ngIf="product.imageUrls.length === 0" class="no-image-placeholder">
          <mat-icon>image_not_supported</mat-icon>
        </div>
      </div>

      <mat-card-header>
        <mat-card-title>{{ product.name }}</mat-card-title>
        <mat-card-subtitle>{{ product.price | currency:'USD' }}</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <p class="product-description">{{ product.description }}</p>
        <p><strong>Quantity:</strong> {{ product.quantity }}</p>
      </mat-card-content>

      <mat-card-actions *ngIf="product.createdByMe">
        <button mat-button (click)="onEdit($event)">EDIT</button>
        <button mat-button color="warn" (click)="onDelete($event)">DELETE</button>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [`
    .product-card {
      display: flex;
      flex-direction: column;
      width: 100%;
      cursor: pointer;
      transition: box-shadow 0.2s ease-in-out;
    }

    .product-card:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .card-image-container {
      width: 100%;
      height: 200px;
      overflow: hidden;
      position: relative;
      background-color: #f0f0f0;
    }

    .card-image-container img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      position: absolute;
      top: 0;
      left: 0;
      opacity: 0;
      transition: opacity 0.5s ease-in-out;
    }

    .card-image-container img.active {
      opacity: 1;
    }

    .no-image-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f0f0f0;
      color: #aaa;
    }

    .no-image-placeholder mat-icon {
      font-size: 50px;
      width: 50px;
      height: 50px;
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

    mat-card-actions button[mat-button]:first-child {
      background-color: #4caf50;
      color: white;
      font-weight: 600;
    }

    mat-card-actions button[mat-button]:first-child:hover {
      background-color: #45a049;
    }

    mat-card-actions button[color="warn"] {
      background-color: #f44336;
      color: white;
      font-weight: 600;
    }

    mat-card-actions button[color="warn"]:hover {
      background-color: #da190b;
    }
  `]
})
export class ProductCard implements OnInit, OnDestroy { // <-- Implement interfaces

  @Input() product: ProductCardDTO | null = null;
  @Output() edit = new EventEmitter<void>();
  @Output() delete = new EventEmitter<void>();

  // --- New Carousel Logic ---
  currentImageIndex = 0;
  imageChangeInterval: any = null;
  // -------------------------

  constructor(private router: Router,
    private productService: ProductService,
    public dialog: MatDialog
  ) { }

  ngOnInit(): void {
    // Start the carousel when the component loads
    this.startImageCarousel();
  }

  ngOnDestroy(): void {
    // Clear the timer when the component is destroyed
    if (this.imageChangeInterval) {
      clearInterval(this.imageChangeInterval);
    }
  }

  startImageCarousel(): void {
    // Only start if there's more than one image
    if (this.product && this.product.imageUrls.length > 1) {
      this.imageChangeInterval = setInterval(() => {
        // This moves to the next image, wrapping around to 0
        if (this.product) {
          this.currentImageIndex = (this.currentImageIndex + 1) % this.product.imageUrls.length;
        }
      }, 3000); // Change image every 3 seconds
    }
  }

  // --- Optional: Pause carousel on hover ---
  @HostListener('mouseenter')
  onMouseEnter(): void {
    if (this.imageChangeInterval) {
      clearInterval(this.imageChangeInterval);
    }
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.startImageCarousel(); // Restart when mouse leaves
  }
  // ------------------------------------------

  // Helper to get the image URL (unchanged)
  getImageUrl(imagePath: string): string {
    return `https://localhost:8443${imagePath}`;
  }

  onCardClick(): void {
    if (this.product) {
      this.router.navigate(['/product', this.product.id]);
    }
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation(); // Stop the card click
    if (!this.product) return;

    // 1. Open the confirmation dialog
    const dialogRef = this.dialog.open(ConfirmDialog, {
      width: '350px',
      data: {
        title: 'Delete Product',
        message: `Are you sure you want to delete "${this.product.name}"? This action cannot be undone.`
      }
    });

    // 2. Listen for the dialog to close
    dialogRef.afterClosed().subscribe(result => {
      // 3. If the user clicked "Delete" (result is true)
      if (result === true && this.product) {
        this.productService.deleteProduct(this.product.id).subscribe({
          next: (response) => {
            console.log(response); // "Product deleted successfully"
            // 4. Emit the (delete) event to tell the parent to refresh
            this.delete.emit();
          },
          error: (err) => {
            console.error('Failed to delete product', err);
            // TODO: Show a snackbar or alert
          }
        });
      }
    });
  }
  onEdit(event: MouseEvent): void {
    event.stopPropagation(); // Stop the card click
    if (!this.product) return;

    // 1. Fetch the *full* product details first
    this.productService.getProductById(this.product.id).subscribe({
      next: (fullProduct: ProductDetailDTO) => {
        // 2. Open the modal
        const dialogRef = this.dialog.open(EditProductModal, {
          width: '600px',
          data: { product: fullProduct }
        });
        // 3. After modal closes, emit the 'edit' event
        dialogRef.afterClosed().subscribe(wasSuccessful => {
          if (wasSuccessful) {
            this.edit.emit(); // <-- Emits void
          }
        });
      },
      error: (err) => {
        console.error('Failed to fetch product details for editing', err);
        alert('Could not open editor. Please try again.');
      }
    });
  }
}
