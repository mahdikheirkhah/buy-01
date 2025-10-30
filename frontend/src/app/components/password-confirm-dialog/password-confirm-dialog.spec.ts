import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PasswordConfirmDialog } from './password-confirm-dialog';

describe('PasswordConfirmDialog', () => {
  let component: PasswordConfirmDialog;
  let fixture: ComponentFixture<PasswordConfirmDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordConfirmDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PasswordConfirmDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
