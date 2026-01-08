export interface MediaUploadResponseDTO {
  fileId: string;
  fileUrl: string;
  productId: string;
}

// ✅ This is the model for your product detail page
export interface ProductDetailDTO {
  productId: string;
  id?: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  sellerId: string;
  sellerFirstName: string;
  sellerLastName: string;
  sellerEmail: string;
  createdByMe: boolean;
  media: MediaUploadResponseDTO[];
}
