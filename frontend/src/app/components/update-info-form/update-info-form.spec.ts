import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UpdateInfoForm } from './update-info-form';

describe('UpdateInfoForm', () => {
  let component: UpdateInfoForm;
  let fixture: ComponentFixture<UpdateInfoForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UpdateInfoForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UpdateInfoForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
