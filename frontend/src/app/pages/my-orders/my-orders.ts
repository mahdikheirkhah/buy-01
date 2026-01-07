import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { Order, OrderStatus } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { Page } from '../../services/product-service';

@Component({
    selector: 'app-my-orders',
    standalone: true,
    imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatPaginatorModule,
        MatChipsModule,
        MatIconModule
    ],
    templateUrl: './my-orders.html',
    styleUrls: ['./my-orders.css']
})
export class MyOrders implements OnInit {
    orders: Order[] = [];
    pageIndex = 0;
    pageSize = 10;
    totalElements = 0;
    isLoading = true;
    userId: string | null = null;

    constructor(
        private orderService: OrderService,
        private authService: AuthService
    ) { }

    ngOnInit(): void {
        this.authService.currentUser$.subscribe(user => {
            if (user?.id) {
                this.userId = user.id;
                this.fetchOrders();
            }
        });
    }

    fetchOrders(): void {
        if (!this.userId) return;

        this.isLoading = true;
        this.orderService.getUserOrders(this.userId, this.pageIndex, this.pageSize)
            .subscribe({
                next: (page: Page<Order>) => {
                    this.orders = page.content.filter(order => order.status !== OrderStatus.PENDING);
                    this.totalElements = page.totalElements;
                    this.isLoading = false;
                },
                error: (err) => {
                    console.error('Failed to fetch orders:', err);
                    this.isLoading = false;
                }
            });
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.fetchOrders();
    }

    getStatusColor(status: OrderStatus): string {
        switch (status) {
            case OrderStatus.PENDING: return 'accent';
            case OrderStatus.PROCESSING: return 'primary';
            case OrderStatus.SHIPPED: return 'primary';
            case OrderStatus.DELIVERED: return 'accent';
            case OrderStatus.CANCELLED: return 'warn';
            default: return '';
        }
    }

    getTotalAmount(order: Order): number {
        return this.orderService.calculateTotal(order.items);
    }

    reorder(orderId: string): void {
        this.orderService.redoOrder(orderId).subscribe({
            next: (newOrder) => {
                alert('Order recreated! Check your cart.');
            },
            error: (err) => {
                console.error('Failed to reorder:', err);
                alert('Failed to recreate order');
            }
        });
    }
}
