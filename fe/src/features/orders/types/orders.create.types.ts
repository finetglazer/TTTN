import { ORDER } from '@/core/config/constants';

export interface Order {
    id: string;
    userId: string;
    userEmail: string;
    userName: string;
    orderDescription: string;
    totalAmount: number;
    shippingAddress: string;
    status: keyof typeof ORDER.STATUS;
    createdAt: string;
    updatedAt: string;
    sagaId?: string;
}

export interface CreateOrderRequest {
    userId: String;
    userEmail: string;
    userName: string;
    orderDescription: string;
    totalAmount: number;
    shippingAddress: string;
}

export interface OrderItem {
    id: string;
    name: string;
    price: number;
    quantity: number;
}

export interface NewItem {
    name: string;
    price: string;
    quantity: number;
}