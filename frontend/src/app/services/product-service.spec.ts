import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProductService, Page } from './product-service';
import { ProductCardDTO } from '../models/productCard.model';
import { ProductDetailDTO } from '../models/product.model';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  // Mock data
  const mockProductCard: ProductCardDTO = {
    id: 'prod-123',
    name: 'Test Product',
    description: 'A great product',
    price: 99.99,
    quantity: 10,
    createdByMe: true,
    imageUrls: ['https://example.com/image1.jpg', 'https://example.com/image2.jpg']
  };

  const mockProductDetail: ProductDetailDTO = {
    productId: 'prod-123',
    name: 'Test Product',
    description: 'A great product',
    price: 99.99,
    quantity: 10,
    sellerFirstName: 'John',
    sellerLastName: 'Doe',
    sellerEmail: 'john@example.com',
    createdByMe: true,
    media: [
      { fileId: 'media-1', fileUrl: 'https://example.com/image1.jpg', productId: 'prod-123' },
      { fileId: 'media-2', fileUrl: 'https://example.com/image2.jpg', productId: 'prod-123' }
    ]
  };

  const mockProductsPage: Page<ProductCardDTO> = {
    content: [mockProductCard],
    totalElements: 5,
    totalPages: 1,
    number: 0
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProductService]
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ============ Service Creation ============
  describe('Service Creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  // ============ Create Product Tests ============
  describe('createProduct()', () => {
    it('should send POST request to create product', () => {
      const productData = {
        name: 'New Product',
        description: 'A new product',
        price: 49.99,
        quantity: 20
      };

      service.createProduct(productData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(productData);
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockProductDetail);
    });

    it('should handle product creation success', () => {
      const productData = { name: 'New Product', price: 49.99 };
      const response = { ...mockProductDetail, name: 'New Product', price: 49.99 };

      service.createProduct(productData).subscribe(result => {
        expect(result).toEqual(response);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      req.flush(response);
    });

    it('should handle product creation error', () => {
      const productData = { name: 'New Product' };

      service.createProduct(productData).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      req.flush('Invalid product data', { status: 400, statusText: 'Bad Request' });
    });
  });

  // ============ Upload Product Image Tests ============
  describe('uploadProductImage()', () => {
    it('should send FormData with image file', () => {
      const mockFile = new File(['image-content'], 'product.jpg', { type: 'image/jpeg' });

      service.uploadProductImage('prod-123', mockFile).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/create/images');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);

      req.flush('media-123');
    });

    it('should include file and productId in FormData', () => {
      const mockFile = new File(['image-content'], 'product.jpg', { type: 'image/jpeg' });

      service.uploadProductImage('prod-123', mockFile).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/create/images');
      const formData = req.request.body as FormData;

      expect(formData.get('productId')).toBe('prod-123');
      expect(formData.has('file')).toBe(true);

      req.flush('media-123');
    });

    it('should return media ID on successful upload', () => {
      const mockFile = new File(['image-content'], 'product.jpg');

      service.uploadProductImage('prod-123', mockFile).subscribe(result => {
        expect(result).toBe('media-123');
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/create/images');
      req.flush('media-123');
    });

    it('should handle image upload error', () => {
      const mockFile = new File(['image-content'], 'product.jpg');

      service.uploadProductImage('prod-123', mockFile).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(413);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/create/images');
      req.flush('File too large', { status: 413, statusText: 'Payload Too Large' });
    });
  });

  // ============ Get My Products Tests ============
  describe('getMyProducts()', () => {
    it('should fetch seller products with pagination', () => {
      service.getMyProducts(0, 10).subscribe();

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/products/my-products'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('sort')).toBe('createdAt,desc');

      req.flush(mockProductsPage);
    });

    it('should return Page<ProductCardDTO>', () => {
      service.getMyProducts(0, 10).subscribe(result => {
        expect(result).toEqual(mockProductsPage);
        expect(result.content.length).toBe(1);
        expect(result.totalElements).toBe(5);
      });

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/products/my-products'
      );
      req.flush(mockProductsPage);
    });

    it('should handle pagination parameters correctly', () => {
      service.getMyProducts(2, 20).subscribe();

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/products/my-products'
      );
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('20');

      req.flush(mockProductsPage);
    });

    it('should handle error when fetching products', () => {
      service.getMyProducts(0, 10).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(401);
        }
      );

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/products/my-products'
      );
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });
  });

  // ============ Get All Products Tests ============
  describe('getAllProducts()', () => {
    it('should fetch all products with pagination', () => {
      service.getAllProducts(0, 10).subscribe();

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/products/all'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');

      req.flush(mockProductsPage);
    });

    it('should return paginated products', () => {
      service.getAllProducts(0, 10).subscribe(result => {
        expect(result).toEqual(mockProductsPage);
        expect(result.content.length).toBe(1);
        expect(result.totalPages).toBe(1);
      });

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/products/all'
      );
      req.flush(mockProductsPage);
    });

    it('should sort by newest first', () => {
      service.getAllProducts(0, 10).subscribe();

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/products/all'
      );
      expect(req.request.params.get('sort')).toBe('createdAt,desc');

      req.flush(mockProductsPage);
    });

    it('should handle empty results', () => {
      const emptyPage: Page<ProductCardDTO> = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0
      };

      service.getAllProducts(0, 10).subscribe(result => {
        expect(result.content.length).toBe(0);
        expect(result.totalElements).toBe(0);
      });

      const req = httpMock.expectOne(request =>
        request.url === 'https://localhost:8443/api/products/all'
      );
      req.flush(emptyPage);
    });
  });

  // ============ Get Product by ID Tests ============
  describe('getProductById()', () => {
    it('should fetch product by ID', () => {
      service.getProductById('prod-123').subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockProductDetail);
    });

    it('should return product details', () => {
      service.getProductById('prod-123').subscribe(result => {
        expect(result).toEqual(mockProductDetail);
        expect(result.name).toBe('Test Product');
        expect(result.price).toBe(99.99);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush(mockProductDetail);
    });

    it('should include media information', () => {
      service.getProductById('prod-123').subscribe(result => {
        expect(result.media.length).toBe(2);
        expect(result.media[0].fileId).toBe('media-1');
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush(mockProductDetail);
    });

    it('should handle product not found error', () => {
      service.getProductById('invalid-id').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/invalid-id');
      req.flush('Product not found', { status: 404, statusText: 'Not Found' });
    });
  });

  // ============ Delete Product Tests ============
  describe('deleteProduct()', () => {
    it('should send DELETE request to remove product', () => {
      service.deleteProduct('prod-123').subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      expect(req.request.method).toBe('DELETE');
      expect(req.request.withCredentials).toBe(true);

      req.flush('Product deleted successfully');
    });

    it('should return success message', () => {
      service.deleteProduct('prod-123').subscribe(result => {
        expect(result).toBe('Product deleted successfully');
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush('Product deleted successfully');
    });

    it('should handle delete error', () => {
      service.deleteProduct('prod-123').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(403);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });

    it('should handle product not found on delete', () => {
      service.deleteProduct('invalid-id').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/invalid-id');
      req.flush('Product not found', { status: 404, statusText: 'Not Found' });
    });
  });

  // ============ Update Product Tests ============
  describe('updateProduct()', () => {
    it('should send PUT request to update product', () => {
      const updateData = { name: 'Updated Product', price: 79.99 };

      service.updateProduct('prod-123', updateData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateData);
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockProductDetail);
    });

    it('should return updated product', () => {
      const updateData = { name: 'Updated Product', price: 79.99 };
      const updatedProduct = { ...mockProductDetail, ...updateData };

      service.updateProduct('prod-123', updateData).subscribe(result => {
        expect(result.name).toBe('Updated Product');
        expect(result.price).toBe(79.99);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush(updatedProduct);
    });

    it('should handle update error', () => {
      const updateData = { name: 'Updated Product' };

      service.updateProduct('prod-123', updateData).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush('Invalid update data', { status: 400, statusText: 'Bad Request' });
    });

    it('should handle unauthorized update attempt', () => {
      const updateData = { name: 'Updated Product' };

      service.updateProduct('prod-123', updateData).subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(403);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });
  });

  // ============ Delete Product Image Tests ============
  describe('deleteProductImage()', () => {
    it('should send DELETE request to remove media', () => {
      service.deleteProductImage('prod-123', 'media-1').subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/deleteMedia/prod-123/media-1');
      expect(req.request.method).toBe('DELETE');
      expect(req.request.withCredentials).toBe(true);

      req.flush({ success: true });
    });

    it('should return success response', () => {
      service.deleteProductImage('prod-123', 'media-1').subscribe(result => {
        expect(result.success).toBe(true);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/deleteMedia/prod-123/media-1');
      req.flush({ success: true });
    });

    it('should handle media not found error', () => {
      service.deleteProductImage('prod-123', 'invalid-media').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/deleteMedia/prod-123/invalid-media');
      req.flush('Media not found', { status: 404, statusText: 'Not Found' });
    });

    it('should handle unauthorized delete attempt', () => {
      service.deleteProductImage('prod-123', 'media-1').subscribe(
        () => fail('should have failed'),
        (error) => {
          expect(error.status).toBe(403);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/deleteMedia/prod-123/media-1');
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });
  });
});

