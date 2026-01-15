import { Component, EventEmitter, Output, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { User } from '../../models/user.model'; // Adjust path if needed
import { AuthService } from '../../services/auth'; // Adjust path if needed
import { OrderService } from '../../services/order.service';

// Import Angular Material modules
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatBadgeModule
  ],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css'] // Use styleUrls (plural)
})
export class Navbar implements OnInit, OnDestroy {
  @Output() toggleSidenav = new EventEmitter<void>();
  public currentUser$: Observable<User | null>;
  public cartItemCount$: Observable<number>;
  private destroy$ = new Subject<void>();

  constructor(private authService: AuthService, private router: Router, private orderService: OrderService) {
    this.currentUser$ = this.authService.currentUser$;
    this.cartItemCount$ = this.orderService.cartItemCount$;
  }

  ngOnInit() {
    // Load cart when user is available
    this.currentUser$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(user => {
      if (user && user.id) {
        console.log('[Navbar] Loading cart for user:', user.id);
        this.orderService.loadCart(user.id).pipe(
          takeUntil(this.destroy$)
        ).subscribe(
          (cart) => console.log('[Navbar] Cart loaded:', cart),
          (error) => console.error('[Navbar] Error loading cart:', error)
        );
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Helper to build the full URL for the avatar
  getAvatarUrl(avatarPath: string): string {
    // Prepends the gateway URL to the path stored in the database
    return `https://localhost:8443${avatarPath}`;
  }

  onLogout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}
