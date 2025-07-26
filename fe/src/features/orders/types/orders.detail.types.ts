// src/types/orders.detail.types.ts
import {PaymentDetail} from "@/features/payments/types/payment.detail.types";

export interface OrderDetail {
    orderId: number;
    userId: string;
    userEmail: string;
    userName: string;
    status: string;
    orderDescription: string;
    totalAmount: number;
    createdAt: string;
}



export interface OrderDetailsData {
    order: OrderDetail;
    payment: PaymentDetail;
}