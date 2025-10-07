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
  templateUrl: './sidenav.html',
  styleUrl: './sidenav.css'
})
export class SidenavComponent {
  @Output() closeSidenav = new EventEmitter<void>();

  constructor(private authService: AuthService, private router: Router) {}

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
