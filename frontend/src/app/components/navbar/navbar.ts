import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { User } from '../../models/user.model'; // Adjust path if needed
import { AuthService } from '../../services/auth'; // Adjust path if needed

// Import Angular Material modules
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule
  ],
  template: `
    <mat-toolbar color="primary" class="navbar-container">
      <button mat-icon-button (click)="toggleSidenav.emit()">
        <mat-icon>menu</mat-icon>
      </button>

      <a routerLink="/home" class="brand-logo">My Site</a>

      <span class="spacer"></span>

      <ng-container *ngIf="currentUser$ | async as user; else loggedOutButtons">
        <button mat-button [matMenuTriggerFor]="userMenu" class="user-menu-button">
          <img *ngIf="user.role === 'SELLER' && user.avatarUrl"
               [src]="getAvatarUrl(user.avatarUrl)"
               alt="User Avatar"
               class="navbar-avatar-image">
          <span class="navbar-user-name">
            {{ user.firstName }} {{ user.lastName }}
          </span>
        </button>

        <mat-menu #userMenu="matMenu">
          <button mat-menu-item routerLink="/my-info">
            <mat-icon>person</mat-icon>
            <span>My Profile</span>
          </button>
          <button mat-menu-item (click)="onLogout()">
            <mat-icon>exit_to_app</mat-icon>
            <span>Logout</span>
          </button>
        </mat-menu>
      </ng-container>

      <ng-template #loggedOutButtons>
        <button mat-button routerLink="/login">Login</button>
        <button mat-raised-button color="accent" routerLink="/register">Register</button>
      </ng-template>
    </mat-toolbar>
  `,
  styles: [`
    button[routerLink="/register"] {
      background-color: var(--green);
      color: var(--white);
    }

    .navbar-container {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 100;
      height: 72px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      display: flex;
      align-items: center;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .brand-logo {
      text-decoration: none;
      color: var(--white);
      font-size: 1.5rem;
      font-weight: 500;
      margin: 0 16px;
    }

    .user-menu-button {
      display: flex;
      align-items: center;
      padding: 0 16px;
      border-radius: 30px;
      min-width: 0;
      height: 48px;
      transition: background-color 0.2s ease;
    }

    .user-menu-button:hover {
      background-color: rgba(255, 255, 255, 0.1);
    }

    .navbar-avatar-image {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      object-fit: cover;
      margin-right: 10px;
      border: 2px solid var(--white);
    }

    .navbar-user-name {
      color: var(--white);
      font-weight: 500;
      font-size: 1rem;
    }
  `]
})
export class Navbar {
  @Output() toggleSidenav = new EventEmitter<void>();
  public currentUser$: Observable<User | null>;

  constructor(private authService: AuthService, private router: Router) {
    this.currentUser$ = this.authService.currentUser$;
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
