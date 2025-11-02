import { HttpClient, HttpParams } from '@angular/common/http'; // Import HttpParams
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ProductCardDTO } from '../models/productCard.model'; // Make sure this path is correct
import {ProductDetailDTO} from '../models/product.model';
// We can define the Page response interface here or in its own file
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // Current page number
}


@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private productApiUrl = 'https://localhost:8443/api/products';
   private mediaApiUrl = 'https://localhost:8443/api/media';
  private imageApiUrl = 'https://localhost:8443/api/products/create/images';

  constructor(private http: HttpClient) { }

  createProduct(productData: any): Observable<any> {
    return this.http.post(this.productApiUrl, productData, { withCredentials: true });
  }

  /**
   * Uploads a single image for a given product.
   * Both the file and the productId are sent in the FormData.
   */
  uploadProductImage(productId: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('productId', productId);

    // We no longer need the productId in the URL itself
    // We also tell the backend to expect 'text'
    return this.http.post(this.imageApiUrl, formData, {
      withCredentials: true,
      responseType: 'text' // Expect a plain text "success" message
    });
  }

  /**
   * Fetches the products created by the currently logged-in seller,
   * with pagination.
   * @param page Page number (0-indexed)
   * @param size Number of items per page
   */
  getMyProducts(page: number, size: number): Observable<Page<ProductCardDTO>> {
    // Create URL parameters
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc'); // Sort by newest first

    return this.http.get<Page<ProductCardDTO>>(`${this.productApiUrl}/my-products`, {
      withCredentials: true,
      params: params // Pass the parameters
    });
  }
  getAllProducts(page: number, size: number): Observable<Page<ProductCardDTO>> {
    // Create URL parameters
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');

    return this.http.get<Page<ProductCardDTO>>(`${this.productApiUrl}/all`, {
      withCredentials: true,
      params: params
    });
  }
  getProductById(id: string): Observable<ProductDetailDTO> {
      return this.http.get<ProductDetailDTO>(`${this.productApiUrl}/${id}`, {
        withCredentials: true
      });
    }
  deleteProduct(id: string): Observable<string> {
      return this.http.delete(`${this.productApiUrl}/${id}`, {
        withCredentials: true,
        responseType: 'text' // Because your backend returns a plain string
      });
  }
  updateProduct(id: string, productData: any): Observable<ProductDetailDTO> {
      return this.http.put<ProductDetailDTO>(`${this.productApiUrl}/${id}`, productData, {
        withCredentials: true
      });
  }
  deleteProductImage(productId: string,mediaId: string): Observable<any> {

      return this.http.delete(`${this.productApiUrl}/deleteMedia/${productId}/${mediaId}`, {
        withCredentials: true,
        responseType: 'json' // Expects a JSON response like { "message": "..." }
      });
  }
}
