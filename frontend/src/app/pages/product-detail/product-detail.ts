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
    MatDialogModule,
    ConfirmDialog,
    EditProductModal
  ],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.css']
})
export class ProductDetail implements OnInit {
  product: ProductDetailDTO | null = null;
  selectedImageUrl: string | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    public dialog: MatDialog
  ) {}

  ngOnInit(): void {
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
