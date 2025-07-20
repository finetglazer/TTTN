export interface Order {
    id: string;
    userId: string;
    userEmail: string;
    userName: string;
    orderDescription: string;
    totalAmount: number;
    status: 'CREATED' | 'CONFIRMED' | 'DELIVERED' | 'CANCELLED';
    createdAt: string;
    updatedAt: string;
    sagaId?: string;
}

export interface CreateOrderRequest {
    userEmail: string;
    userName: string;
    orderDescription: string;
    totalAmount: number;
}