import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { ConfirmDialog, ConfirmDialogData } from './confirm-dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

describe('ConfirmDialog', () => {
  let component: ConfirmDialog;
  let fixture: ComponentFixture<ConfirmDialog>;
  let dialogRefMock: jasmine.SpyObj<MatDialogRef<ConfirmDialog>>;
  const mockData: ConfirmDialogData = {
    title: 'Confirm Action',
    message: 'Are you sure you want to proceed?'
  };

  beforeEach(async () => {
    dialogRefMock = jasmine.createSpyObj('MatDialogRef', ['close']);

    // Override component to use inline template
    TestBed.overrideComponent(ConfirmDialog, {
      set: {
        template: `
          <h2>{{ data.title }}</h2>
          <p>{{ data.message }}</p>
          <button (click)="onConfirm()">OK</button>
          <button (click)="onCancel()">Cancel</button>
        `,
        styles: []
      }
    });

    await TestBed.configureTestingModule({
      imports: [ConfirmDialog, CommonModule, MatDialogModule, MatButtonModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: mockData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display provided title and message', () => {
    expect(component.data.title).toBe('Confirm Action');
    expect(component.data.message).toBe('Are you sure you want to proceed?');
  });

  it('should close dialog with false on cancel', () => {
    component.onCancel();

    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it('should close dialog with true on confirm', () => {
    component.onConfirm();

    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it('should have dialogRef injected', () => {
    expect(component.dialogRef).toBeTruthy();
  });
});
