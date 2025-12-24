import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ProductDetail } from './product-detail';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';

describe('ProductDetail', () => {
  let component: ProductDetail;

  beforeEach(async () => {
    const authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.currentUser$ = of(null);

    const productServiceMock = jasmine.createSpyObj('ProductService', ['getProductById']);
    productServiceMock.getProductById.and.returnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [ProductDetail, HttpClientTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { paramMap: of(new Map([['id', 'test-id']])) } },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(ProductDetail);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
