export interface MediaUploadResponseDTO {
  fileId: string;
  fileUrl: string;
  productId: string;
}

// ✅ This is the model for your product detail page
export interface ProductDetailDTO {
  productId: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  sellerFirstName: string;
  sellerLastName: string;
  sellerEmail: string;
  createdByMe: boolean;
  media: MediaUploadResponseDTO[];
}
