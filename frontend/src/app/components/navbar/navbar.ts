import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
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
export class Navbar {
  @Output() toggleSidenav = new EventEmitter<void>();
  public currentUser$: Observable<User | null>;
  public cartItemCount$: Observable<number>;

  constructor(private authService: AuthService, private router: Router, private orderService: OrderService) {
    this.currentUser$ = this.authService.currentUser$;
    this.cartItemCount$ = this.orderService.cartItemCount$;
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
