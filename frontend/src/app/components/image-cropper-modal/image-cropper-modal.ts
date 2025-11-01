import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ImageCropperComponent, ImageCroppedEvent } from 'ngx-image-cropper';

@Component({
  selector: 'app-image-cropper-modal',
  standalone: true,
  imports: [CommonModule, ImageCropperComponent],
  templateUrl: './image-cropper-modal.html',
  styleUrls: ['./image-cropper-modal.css']
})
export class ImageCropperModal {

  // 1. INPUT: Receives the file selection event from the parent
  @Input() imageChangedEvent: any = '';

  // 2. OUTPUTS: Emits the final blob or a cancel event
  @Output() croppedImageBlob = new EventEmitter<Blob>();
  @Output() modalClosed = new EventEmitter<void>();

  croppedImage: any = '';
  croppedBlob: Blob | null = null;

  constructor() {}

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
