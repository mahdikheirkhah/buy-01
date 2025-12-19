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
import { AuthGuard } from './guards/auth-guard';
import { ProductDetail } from './pages/product-detail/product-detail';

export const routes: Routes = [
  {
    path: 'auth',
    component: AuthLayout,
    canActivate: [AuthGuard],
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [LoggedInGuard],
    children: [
      { path: 'home', component: HomeComponent },
      { path: 'my-info', component: MyInfo },
      { path: 'my-products', component: MyProducts },
      { path: 'create-product', component: CreateProduct },
      { path: 'product/:id', component: ProductDetail },
      { path: '', redirectTo: 'home', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'auth/login' }
];
