import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { SidenavComponent } from '../../components/sidenav/sidenav.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    MatSidenavModule,
    NavbarComponent,
    SidenavComponent
  ],
  template: `
    <mat-sidenav-container class="sidenav-container">
      <mat-sidenav #sidenav mode="over" class="sidenav">
        <app-sidenav (closeSidenav)="sidenav.close()"></app-sidenav>
      </mat-sidenav>

      <mat-sidenav-content>
        <app-navbar (toggleSidenav)="sidenav.toggle()"></app-navbar>
        <main>
          <router-outlet></router-outlet>
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .sidenav-container {
      height: 100vh;
    }

    main {
      padding-top: 64px;
    }

    .sidenav {
      width: 250px;
    }
  `]
})
export class MainLayout { }