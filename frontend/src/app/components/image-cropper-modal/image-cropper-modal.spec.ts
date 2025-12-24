import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ImageCropperModal } from './image-cropper-modal';

describe('ImageCropperModal', () => {
  let component: ImageCropperModal;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImageCropperModal],
      providers: [
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: {} }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(ImageCropperModal);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
