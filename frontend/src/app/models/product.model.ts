export interface MediaUploadResponseDTO {
  id: string;
  fileUrl: string;
  // Add other fields if they exist, like createdAt
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
