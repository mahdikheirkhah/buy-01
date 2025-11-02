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
import { ProductDetailDTO,MediaUploadResponseDTO } from '../../models/product.model';
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
  templateUrl: './product-card.html',
  styleUrls: ['./product-card.css']
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
    ) {}

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
