// Order models
export interface OrderItem {
    productId: string;
    productName: string;
    sellerId: string;
    quantity: number;
    unitPrice: number;
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
}

export enum OrderStatus {
    PENDING = 'PENDING',
    PROCESSING = 'PROCESSING',
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

export interface UpdateOrderStatusRequest {
    status: OrderStatus;
}
