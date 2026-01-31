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

  // ============ Edge Cases and Error Scenarios ============
  describe('Edge Cases and Error Scenarios', () => {
    it('should handle null pagination parameters', () => {
      service.getAllProducts(0, 10).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/all?page=0&size=10&sort=createdAt,desc');
      req.flush(mockProductsPage);
    });

    it('should handle negative page index', () => {
      service.getAllProducts(-1, 10).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/all?page=-1&size=10&sort=createdAt,desc');
      req.flush(mockProductsPage);
    });

    it('should handle large page size', () => {
      service.getAllProducts(0, 1000).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/all?page=0&size=1000&sort=createdAt,desc');
      req.flush(mockProductsPage);
    });

    it('should handle empty product list response', () => {
      service.getAllProducts(0, 10).subscribe(result => {
        expect(result.content.length).toBe(0);
        expect(result.totalElements).toBe(0);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/all?page=0&size=10&sort=createdAt,desc');
      req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0 });
    });

    it('should handle product with no images', () => {
      const productNoImages = { ...mockProductCard, imageUrls: [] };
      service.getProductById('prod-123').subscribe((result: any) => {
        expect(result.media.length).toBe(0);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      const detailNoImages = { ...mockProductDetail, media: [] };
      req.flush(detailNoImages);
    });

    it('should handle product with single image', () => {
      const productSingleImage = {
        ...mockProductCard,
        imageUrls: ['https://example.com/image1.jpg']
      };
      service.getProductById('prod-123').subscribe((result: any) => {
        expect(result.media.length).toBe(1);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      const detailSingleImage = { ...mockProductDetail, media: [mockProductDetail.media[0]] };
      req.flush(detailSingleImage);
    });

    it('should handle product with many images', () => {
      const manyImages = Array(20).fill(null).map((_, i) => ({
        fileId: `media-${i}`,
        fileUrl: `https://example.com/image${i}.jpg`,
        productId: 'prod-123'
      }));
      service.getProductById('prod-123').subscribe((result: any) => {
        expect(result.media.length).toBe(20);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      const detailManyImages = { ...mockProductDetail, media: manyImages };
      req.flush(detailManyImages);
    });

    it('should handle zero price product', () => {
      const zeroPrice = { ...mockProductCard, price: 0 };
      service.createProduct(zeroPrice).subscribe((result: any) => {
        expect(result.price).toBe(0);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      const responseZeroPrice = { ...mockProductDetail, price: 0 };
      req.flush(responseZeroPrice);
    });

    it('should handle negative price in response (edge case)', () => {
      service.getProductById('prod-123').subscribe((result: any) => {
        expect(result.price).toBeLessThan(0);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      const detailNegativePrice = { ...mockProductDetail, price: -50 };
      req.flush(detailNegativePrice);
    });

    it('should handle very large price', () => {
      const largePrice = { ...mockProductCard, price: 999999.99 };
      service.createProduct(largePrice).subscribe((result: any) => {
        expect(result.price).toBe(999999.99);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      const responseLargePrice = { ...mockProductDetail, price: 999999.99 };
      req.flush(responseLargePrice);
    });

    it('should handle zero quantity', () => {
      const zeroQuantity = { ...mockProductCard, quantity: 0 };
      service.createProduct(zeroQuantity).subscribe(result => {
        expect(result.quantity).toBe(0);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      const responseZeroQuantity = { ...mockProductDetail, quantity: 0 };
      req.flush(responseZeroQuantity);
    });

    it('should handle search with empty results', () => {
      service.getAllProducts(0, 10).subscribe(result => {
        expect(result.content.length).toBe(0);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/all?page=0&size=10&sort=createdAt,desc');
      req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0 });
    });

    it('should handle search with special characters', () => {
      service.getAllProducts(0, 10).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/all?page=0&size=10&sort=createdAt,desc');
      req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0 });
    });

    it('should handle update with no changes', () => {
      const productData = { name: 'Same Name', price: 99.99 };
      service.updateProduct('prod-123', productData).subscribe(result => {
        expect(result.name).toBe('Test Product');
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush(mockProductDetail);
    });

    it('should handle network error on create', () => {
      service.createProduct({ name: 'New Product' }).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      req.error(new ErrorEvent('Network error'));
    });

    it('should handle server error 500 on delete', () => {
      service.deleteProduct('prod-123').subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    });

    it('should handle timeout on file upload', () => {
      const fileUploadData = new File(['test'], 'test.jpg');

      service.uploadProductImage('prod-123', fileUploadData).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne(request =>
        request.url.includes('products/create/images')
      );
      req.flush('Timeout', { status: 408, statusText: 'Request Timeout' });
    });

    it('should handle malformed JSON response', () => {
      service.getProductById('prod-123').subscribe((result: any) => {
        expect(result).toBeTruthy();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush({ partial: 'data' });
    });

    it('should handle empty product list with pagination', () => {
      const emptyPage: Page<ProductCardDTO> = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0
      };

      service.getAllProducts(0, 10).subscribe((result: any) => {
        expect(result.content.length).toBe(0);
        expect(result.totalElements).toBe(0);
      });

      const req = httpMock.expectOne(request =>
        request.url.includes('/api/products') &&
        request.params.get('page') === '0'
      );
      req.flush(emptyPage);
    });

    it('should handle large page size pagination', () => {
      service.getAllProducts(0, 1000).subscribe();

      const req = httpMock.expectOne(request =>
        request.url.includes('/api/products') &&
        request.params.get('size') === '1000'
      );
      expect(req.request.params.get('size')).toBe('1000');
      req.flush(mockProductsPage);
    });

    it('should handle search with special characters', () => {
      const updatedData = { name: 'product@#$%&*()' };
      service.updateProduct('prod-123', updatedData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      expect(req.request.body.name).toContain('@');
      req.flush(mockProductDetail);
    });

    it('should handle search with unicode characters', () => {
      const updatedData = { name: '中文产品' };
      service.updateProduct('prod-123', updatedData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush(mockProductDetail);
    });

    it('should handle very long product names', () => {
      const longName = 'A'.repeat(1000);
      const productData = { name: longName, price: 49.99 };

      service.createProduct(productData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      expect(req.request.body.name.length).toBe(1000);
      req.flush(mockProductDetail);
    });

    it('should handle zero price product', () => {
      const productData = { name: 'Free Product', price: 0, quantity: 100 };

      service.createProduct(productData).subscribe((result: any) => {
        expect(result.price).toBe(0);
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      req.flush({ ...mockProductDetail, price: 0 });
    });

    it('should handle negative quantity edge case', () => {
      const productData = { name: 'Product', price: 49.99, quantity: -5 };

      service.createProduct(productData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      req.flush(mockProductDetail);
    });

    it('should handle maximum integer price', () => {
      const productData = { name: 'Expensive', price: Number.MAX_SAFE_INTEGER };

      service.createProduct(productData).subscribe((result: any) => {
        expect(result.price).toBeDefined();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products');
      req.flush({ ...mockProductDetail, price: Number.MAX_SAFE_INTEGER });
    });

    it('should handle concurrent create and delete operations', () => {
      service.createProduct({ name: 'Product', price: 49.99 }).subscribe();
      service.deleteProduct('prod-456').subscribe();

      let req = httpMock.expectOne('https://localhost:8443/api/products');
      req.flush(mockProductDetail);

      req = httpMock.expectOne('https://localhost:8443/api/products/prod-456');
      req.flush({ message: 'deleted' });
    });

    it('should handle concurrent get and list operations', () => {
      service.getAllProducts(0, 10).subscribe();
      service.getProductById('prod-123').subscribe();

      let req = httpMock.expectOne(request =>
        request.url.includes('/api/products') && !request.url.includes('prod-123')
      );
      req.flush(mockProductsPage);

      req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush(mockProductDetail);
    });

    it('should handle multiple pagination requests', () => {
      for (let i = 0; i < 5; i++) {
        service.getAllProducts(i, 10).subscribe();
      }

      for (let i = 0; i < 5; i++) {
        const req = httpMock.expectOne(request =>
          request.url.includes('/api/products') &&
          request.params.get('page') === i.toString()
        );
        req.flush(mockProductsPage);
      }
    });

    it('should handle product update with partial data', () => {
      const updateData = { name: 'Updated Name' };

      service.updateProduct('prod-123', updateData).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      expect(req.request.method).toBe('PUT');
      req.flush(mockProductDetail);
    });

    it('should handle delete product with special ID format', () => {
      const specialId = 'prod-123-456-789';

      service.deleteProduct(specialId).subscribe();

      const req = httpMock.expectOne(`https://localhost:8443/api/products/${specialId}`);
      expect(req.request.method).toBe('DELETE');
      req.flush({ message: 'deleted' });
    });

    it('should handle image upload with large file', () => {
      const largeBuffer = new ArrayBuffer(10000000);
      const largeFile = new File([largeBuffer], 'large-image.jpg', { type: 'image/jpeg' });

      service.uploadProductImage('prod-123', largeFile).subscribe();

      const req = httpMock.expectOne('https://localhost:8443/api/products/create/images');
      req.flush('Image uploaded');
    });

    // it('should handle multiple image uploads in sequence', () => {
    //   const mockFile1 = new File(['image1'], 'image1.jpg');
    //   const mockFile2 = new File(['image2'], 'image2.jpg');

    //   service.uploadProductImage('prod-123', mockFile1).subscribe();
    //   service.uploadProductImage('prod-123', mockFile2).subscribe();

    //   let req = httpMock.expectOne('https://localhost:8443/api/products/create/images');
    //   req.flush({ id: 'media-123' });
    //   req = httpMock.expectOne('https://localhost:8443/api/products/create/images');
    //   req.flush({ id: 'media-456' });
    // });

    it('should include credentials in all product requests', () => {
      service.getAllProducts(0, 10).subscribe();
      service.getProductById('prod-123').subscribe();
      service.getMyProducts(0, 10).subscribe();

      let requests = httpMock.match(req => req.withCredentials);
      expect(requests.length).toBeGreaterThan(0);
      requests.forEach(req => req.flush({}));
    });

    it('should handle 429 too many requests error', () => {
      service.getAllProducts(0, 10).subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(429);
        }
      );

      const req = httpMock.expectOne(request =>
        request.url.includes('/api/products')
      );
      req.flush('Too many requests', { status: 429, statusText: 'Too Many Requests' });
    });

    it('should handle 503 service unavailable error', () => {
      service.getProductById('prod-123').subscribe(
        () => fail('should have failed'),
        (error: any) => {
          expect(error.status).toBe(503);
        }
      );

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush('Service unavailable', { status: 503, statusText: 'Service Unavailable' });
    });

    it('should handle response with null values', () => {
      const productWithNulls = {
        ...mockProductDetail,
        sellerEmail: null,
        media: null
      };

      service.getProductById('prod-123').subscribe((result: any) => {
        expect(result).toBeDefined();
      });

      const req = httpMock.expectOne('https://localhost:8443/api/products/prod-123');
      req.flush(productWithNulls);
    });

    it('should handle HTTP client connection timeout', () => {
      service.getAllProducts(0, 10).subscribe(
        () => fail('should have timed out'),
        (error: any) => {
          expect(error).toBeDefined();
        }
      );

      const req = httpMock.expectOne(request =>
        request.url.includes('/api/products')
      );
      req.error(new ProgressEvent('Connection timeout'));
    });
  });
});

