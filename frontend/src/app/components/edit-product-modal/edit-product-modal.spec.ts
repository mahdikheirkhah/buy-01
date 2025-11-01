import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditProductModal } from './edit-product-modal';

describe('EditProductModal', () => {
  let component: EditProductModal;
  let fixture: ComponentFixture<EditProductModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditProductModal]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditProductModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
