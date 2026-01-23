import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ImageCropperComponent, ImageCroppedEvent } from 'ngx-image-cropper';

@Component({
  selector: 'app-image-cropper-modal',
  standalone: true,
  imports: [CommonModule, ImageCropperComponent],
  template: `
    <div class="cropper-modal-overlay">
      <div class="cropper-modal-content">
        <h3>Crop Your Image</h3>

        <div class="cropper-wrapper">
          <image-cropper
            [imageChangedEvent]="imageChangedEvent"
            [maintainAspectRatio]="true"
            [aspectRatio]="1 / 1"
            [roundCropper]="true"
            [resizeToWidth]="256"
            format="png"
            (imageCropped)="imageCropped($event)"
            (loadImageFailed)="loadImageFailed()"
          ></image-cropper>
        </div>

        <div class="preview-container" *ngIf="croppedImage">
          <p>Preview:</p>
          <img [src]="croppedImage" alt="Cropped image preview" />
        </div>

        <div class="cropper-buttons">
          <button type="button" (click)="cancelCrop()" class="cancel-btn">Cancel</button>
          <button type="button" (click)="saveCrop()" class="save-btn" [disabled]="!croppedBlob">Save</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .cropper-modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.7);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
    }

    .cropper-modal-content {
      background-color: white;
      padding: 20px;
      border-radius: 8px;
      width: 90%;
      max-width: 500px;
    }

    .cropper-modal-content h3 {
      margin-top: 0;
      text-align: center;
    }

    .cropper-wrapper {
      width: 100%;
      height: 300px;
      margin-bottom: 15px;
    }

    .preview-container {
      text-align: center;
    }

    .preview-container p {
      font-weight: 500;
      margin-bottom: 5px;
    }

    .preview-container img {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      object-fit: cover;
      border: 2px solid #ddd;
    }

    .cropper-buttons {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }
  `]
})
export class ImageCropperModal {

  // 1. INPUT: Receives the file selection event from the parent
  @Input() imageChangedEvent: any = '';

  // 2. OUTPUTS: Emits the final blob or a cancel event
  @Output() croppedImageBlob = new EventEmitter<Blob>();
  @Output() modalClosed = new EventEmitter<void>();

  croppedImage: any = '';
  croppedBlob: Blob | null = null;

  constructor() { }

  // This is called every time the user moves the cropper
  imageCropped(event: ImageCroppedEvent) {
    this.croppedBlob = event.blob ?? null;
    this.croppedImage = event.base64; // For the preview
  }

  loadImageFailed() {
    console.error('Image failed to load in cropper');
    this.modalClosed.emit();
  }

  // User clicks "Save"
  saveCrop() {
    if (this.croppedBlob) {
      this.croppedImageBlob.emit(this.croppedBlob);
    }
    this.modalClosed.emit();
  }

  // User clicks "Cancel"
  cancelCrop() {
    this.modalClosed.emit();
  }
}
