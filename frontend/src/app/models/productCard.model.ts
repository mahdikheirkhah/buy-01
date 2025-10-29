export interface ProductCardDTO {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  createdByMe: boolean;
  imageUrls: string[];
}
