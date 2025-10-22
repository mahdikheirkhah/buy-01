import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private productApiUrl = 'https://localhost:8443/api/products';
  private imageApiUrl = 'https://localhost:8443/api/products/create/images/';

  constructor(private http: HttpClient) { }

  createProduct(productData: any): Observable<any> {
    return this.http.post(this.productApiUrl, productData, { withCredentials: true });
  }

  uploadProductImage(productId: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(this.imageApiUrl + productId, formData, { withCredentials: true });
  }
}
