import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

export interface AddToCartDialogData {
    productId: string;
    productName: string;
    price: number;
    availableStock: number;
    sellerId: string;
}

@Component({
    selector: 'app-add-to-cart-dialog',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatDialogModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule
    ],
    templateUrl: './add-to-cart-dialog.html',
    styleUrls: ['./add-to-cart-dialog.css']
})
export class AddToCartDialog {
    quantity: number = 1;

    constructor(
        public dialogRef: MatDialogRef<AddToCartDialog>,
        @Inject(MAT_DIALOG_DATA) public data: AddToCartDialogData
    ) { }

    onCancel(): void {
        this.dialogRef.close();
    }

    onConfirm(): void {
        if (this.quantity < 1 || this.quantity > this.data.availableStock) {
            return;
        }
        this.dialogRef.close(this.quantity);
    }

    increaseQuantity(): void {
        if (this.quantity < this.data.availableStock) {
            this.quantity++;
        }
    }

    decreaseQuantity(): void {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }

    getTotalPrice(): number {
        return this.quantity * this.data.price;
    }
}
