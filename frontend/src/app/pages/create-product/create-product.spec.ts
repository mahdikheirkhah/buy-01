import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { CreateProduct } from './create-product';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';

describe('CreateProduct', () => {
  let component: CreateProduct;

  beforeEach(async () => {
    const authServiceMock = jasmine.createSpyObj('AuthService', ['logout']);
    authServiceMock.currentUser$ = of(null);

    const productServiceMock = jasmine.createSpyObj('ProductService', ['createProduct', 'uploadProductImage']);

    await TestBed.configureTestingModule({
      imports: [CreateProduct, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(CreateProduct);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
