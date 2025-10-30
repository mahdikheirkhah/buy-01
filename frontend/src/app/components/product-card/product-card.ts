// src/app/components/product-card/product-card.component.ts
import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ProductCardDTO } from '../../models/productCard.model'; // Adjust path
import { CurrencyPipe } from '@angular/common'; // For the price
import { Router, RouterLink } from '@angular/router';
@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    CurrencyPipe,
    RouterLink
  ],
  templateUrl: './product-card.html',
  styleUrls: ['./product-card.css']
})
export class ProductCard implements OnInit, OnDestroy { // <-- Implement interfaces

  @Input() product: ProductCardDTO | null = null;
  @Output() edit = new EventEmitter<string>();
  @Output() delete = new EventEmitter<string>();

  // --- New Carousel Logic ---
  currentImageIndex = 0;
  imageChangeInterval: any = null;
  // -------------------------

  constructor(private router: Router) {}

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

    onEdit(event: MouseEvent): void {
      event.stopPropagation();
      if (this.product) {
        this.edit.emit(this.product.id);
      }
    }

    onDelete(event: MouseEvent): void {
      event.stopPropagation();
      if (this.product) {
        this.delete.emit(this.product.id);
      }
    }
}
