import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { MyProducts } from './my-products';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';

describe('MyProducts', () => {
  let component: MyProducts;

  beforeEach(async () => {
    const authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.currentUser$ = of(null);

    const productServiceMock = jasmine.createSpyObj('ProductService', ['getMyProducts']);
    productServiceMock.getMyProducts.and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0
    }));

    await TestBed.configureTestingModule({
      imports: [MyProducts, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(MyProducts);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
