// src/app/components/product-card/product-card.component.ts
import { Component, Input, Output, EventEmitter }from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ProductCardDTO } from '../../models/productCard.model'; // Adjust path
import { CurrencyPipe } from '@angular/common'; // For the price

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    CurrencyPipe
  ],
  templateUrl: './product-card.html',
  styleUrls: ['./product-card.css']
})
export class ProductCard {

  // 1. INPUT: Receive the product data
  @Input() product: ProductCardDTO | null = null;

  // 2. OUTPUTS: Send events when buttons are clicked
  @Output() edit = new EventEmitter<string>();
  @Output() delete = new EventEmitter<string>();

  constructor() {}

  // 3. Helper to get the image URL
  getImageUrl(imagePath: string): string {
    //console.log("image path", imagePath);
    return `https://localhost:8443${imagePath}`;
  }

  // 4. Methods to emit events
  onEdit(): void {
    if (this.product) {
      this.edit.emit(this.product.id);
    }
  }

  onDelete(): void {
    if (this.product) {
      this.delete.emit(this.product.id);
    }
  }
}
