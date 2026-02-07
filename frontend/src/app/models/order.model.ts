// Order models
export interface OrderItem {
    productId: string;
    quantity: number;
    price?: number;
    productName?: string;
    sellerId?: string;
    imageUrl?: string;
}

export interface Order {
    id: string;
    userId: string;
    shippingAddress: string;
    status: OrderStatus;
    items: OrderItem[];
    paymentMethod: PaymentMethod;
    orderDate: string;
    createdAt: string;
    updatedAt: string;
    isRemoved: boolean;
}

export enum OrderStatus {
    PENDING = 'PENDING',
    PROCESSING = 'PROCESSING',
    SHIPPING = 'SHIPPING',
    SHIPPED = 'SHIPPED',
    DELIVERED = 'DELIVERED',
    CANCELLED = 'CANCELLED'
}

export enum PaymentMethod {
    CARD = 'CARD',
    PAY_ON_DELIVERY = 'PAY_ON_DELIVERY'
}

export interface CreateOrderRequest {
    userId: string;
    shippingAddress: string;
    items: OrderItem[];
    paymentMethod: PaymentMethod;
}

export interface CheckoutRequest {
    shippingAddress: string;
    paymentMethod: PaymentMethod;
}

export interface UpdateOrderStatusRequest {
    status: OrderStatus;
}

export interface RedoOrderResponse {
    order: Order | null;
    message: string;
    outOfStockProducts: string[];
    partiallyFilledProducts: string[];
}