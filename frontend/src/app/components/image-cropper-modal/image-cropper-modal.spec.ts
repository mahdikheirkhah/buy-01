import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImageCropperModal } from './image-cropper-modal';

describe('ImageCropperModal', () => {
  let component: ImageCropperModal;
  let fixture: ComponentFixture<ImageCropperModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImageCropperModal]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ImageCropperModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
