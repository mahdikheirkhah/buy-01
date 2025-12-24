import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';
import { HomeComponent } from './home';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';

describe('Home', () => {
  let component: HomeComponent;

  beforeEach(async () => {
    const authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.currentUser$ = of(null);

    const productServiceMock = jasmine.createSpyObj('ProductService', ['getAllProducts']);
    productServiceMock.getAllProducts.and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0
    }));

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
