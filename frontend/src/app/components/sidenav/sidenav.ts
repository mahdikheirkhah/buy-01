import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../services/auth';
@Component({
  selector: 'app-sidenav',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatListModule,
    MatDividerModule
  ],
  template: `
    <mat-nav-list>
      <a mat-list-item routerLink="/my-info" (click)="closeSidenav.emit()">My Information</a>
      <ng-container *ngIf="isSeller()">
        <a mat-list-item routerLink="/my-products" (click)="closeSidenav.emit()">My Products</a>
        <a mat-list-item routerLink="/create-product" (click)="closeSidenav.emit()">Create a new product</a>
      </ng-container>
      <mat-divider></mat-divider>
      <a mat-list-item (click)="logout()">Logout</a>
    </mat-nav-list>
  `,
  styles: []
})
export class SidenavComponent {
  @Output() closeSidenav = new EventEmitter<void>();

  constructor(private authService: AuthService, private router: Router) { }

  isSeller(): boolean {
    return this.authService.currentUserRole === 'SELLER';
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.closeSidenav.emit();
      this.router.navigate(['/login']);
    });
  }
}
