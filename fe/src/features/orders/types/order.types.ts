import { ORDER } from '@/core/config/constants';

export interface Order {
    id: string;
    userId: string;
    userEmail: string;
    userName: string;
    orderDescription: string;
    totalAmount: number;
    status: keyof typeof ORDER.STATUS;
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