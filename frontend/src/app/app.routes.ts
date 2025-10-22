// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home';
import { LoginComponent } from './pages/login/login';
import { RegisterComponent } from './pages/register/register';
import { MyInfo } from './pages/my-info/my-info';
import { MyProducts } from './pages/my-products/my-products';
import { CreateProduct } from './pages/create-product/create-product';
import { MainLayout } from './layouts/main-layout/main-layout';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { LoggedInGuard } from './guards/logged-in-guard';
import { AuthGuard } from './guards/auth-guard'; // Import AuthGuard

export const routes: Routes = [
  {
    path: '',
    component: MainLayout, // Layout for logged-in users
    canActivate: [LoggedInGuard], // Protects this whole section
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      { path: 'home', component: HomeComponent },
      { path: 'my-info', component: MyInfo },
      { path: 'my-products', component: MyProducts },
      { path: 'create-product', component: CreateProduct },
    ]
  },
  {
    path: '',
    component: AuthLayout, // Layout for login/register
    // Apply AuthGuard here to prevent access if already logged in
    canActivate: [AuthGuard],
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent }
    ]
  },
  // Fallback route (often redirects to login or home depending on auth state)
  { path: '**', redirectTo: 'home' } // Or maybe check auth state here? For now, home is fine.
];
