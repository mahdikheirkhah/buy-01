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
export const routes: Routes = [
  {
    path: '',
    component: MainLayout, // The layout with the navbar/sidenav
    canActivate: [LoggedInGuard], // We will add this in the next step
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
    component: AuthLayout, // The simple layout with NO navbar
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent }
    ]
  },
  // A fallback route
  { path: '**', redirectTo: 'home' }
];