import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home';
import { LoginComponent } from './pages/login/login';
import { RegisterComponent } from './pages/register/register';
import { MyInfo } from './pages/my-info/my-info';
import { MyProducts } from './pages/my-products/my-products';
import { CreateProduct } from './pages/create-product/create-product';
export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'my-info', component: MyInfo }, // Add new routes
  { path: 'my-products', component: MyProducts },
  { path: 'create-product', component: CreateProduct },
  { path: '**', redirectTo: 'home' } // Redirect any other path to home
];