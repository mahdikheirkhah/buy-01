import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ProductCard } from './product-card';
import { ProductService } from '../../services/product-service';

describe('ProductCard', () => {
  let component: ProductCard;

  beforeEach(async () => {
    const productServiceMock = jasmine.createSpyObj('ProductService', ['deleteProduct']);

    await TestBed.configureTestingModule({
      imports: [ProductCard, HttpClientTestingModule],
      providers: [
        { provide: ProductService, useValue: productServiceMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(ProductCard);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
