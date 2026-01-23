import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ImageCropperModal } from './image-cropper-modal';
import { ImageCroppedEvent } from 'ngx-image-cropper';
import { CommonModule } from '@angular/common';

describe('ImageCropperModal', () => {
  let component: ImageCropperModal;
  let fixture: ComponentFixture<ImageCropperModal>;

  beforeEach(async () => {
    // Override component to use inline template
    TestBed.overrideComponent(ImageCropperModal, {
      set: {
        template: `
          <img-cropper [imageChangedEvent]="imageChangedEvent"
            (imageCropped)="imageCropped($event)"
            (imageLoaded)="imageLoaded()"
            (loadImageFailed)="loadImageFailed()">
          </img-cropper>
          <button (click)="saveCrop()">Save</button>
          <button (click)="closeCropper()">Cancel</button>
        `,
        templateUrl: undefined
      }
    });

    await TestBed.configureTestingModule({
      imports: [ImageCropperModal, CommonModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ImageCropperModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty imageChangedEvent', () => {
    expect(component.imageChangedEvent).toBe('');
  });

  it('should initialize with null croppedBlob', () => {
    expect(component.croppedBlob).toBeNull();
  });

  it('should set croppedBlob on imageCropped event', () => {
    const mockBlob = new Blob(['test'], { type: 'image/jpeg' });
    const mockEvent: ImageCroppedEvent = {
      blob: mockBlob,
      base64: 'data:image/jpeg;base64,test',
      width: 100,
      height: 100,
      cropperPosition: { x1: 0, y1: 0, x2: 100, y2: 100 },
      imagePosition: { x1: 0, y1: 0, x2: 100, y2: 100 }
    };

    component.imageCropped(mockEvent);

    expect(component.croppedBlob).toBe(mockBlob);
    expect(component.croppedImage).toBe('data:image/jpeg;base64,test');
  });

  it('should handle imageCropped with null blob', () => {
    const mockEvent: ImageCroppedEvent = {
      blob: null,
      base64: 'data:image/jpeg;base64,test',
      width: 100,
      height: 100,
      cropperPosition: { x1: 0, y1: 0, x2: 100, y2: 100 },
      imagePosition: { x1: 0, y1: 0, x2: 100, y2: 100 }
    };

    component.imageCropped(mockEvent);

    expect(component.croppedBlob).toBeNull();
  });

  it('should emit modalClosed on loadImageFailed', () => {
    spyOn(component.modalClosed, 'emit');
    spyOn(console, 'error');

    component.loadImageFailed();

    expect(console.error).toHaveBeenCalledWith('Image failed to load in cropper');
    expect(component.modalClosed.emit).toHaveBeenCalled();
  });

  it('should emit croppedImageBlob and modalClosed on saveCrop', () => {
    const mockBlob = new Blob(['test'], { type: 'image/jpeg' });
    component.croppedBlob = mockBlob;
    spyOn(component.croppedImageBlob, 'emit');
    spyOn(component.modalClosed, 'emit');

    component.saveCrop();

    expect(component.croppedImageBlob.emit).toHaveBeenCalledWith(mockBlob);
    expect(component.modalClosed.emit).toHaveBeenCalled();
  });

  it('should not emit croppedImageBlob if blob is null on saveCrop', () => {
    component.croppedBlob = null;
    spyOn(component.croppedImageBlob, 'emit');
    spyOn(component.modalClosed, 'emit');

    component.saveCrop();

    expect(component.croppedImageBlob.emit).not.toHaveBeenCalled();
    expect(component.modalClosed.emit).toHaveBeenCalled();
  });

  it('should emit modalClosed on cancelCrop', () => {
    spyOn(component.modalClosed, 'emit');

    component.cancelCrop();

    expect(component.modalClosed.emit).toHaveBeenCalled();
  });
});
