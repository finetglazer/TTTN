// src/validations/orders.detail.schema.ts
import { z } from 'zod';

// Schema for parsing BE response
const OrderDetailSchema = z.object({
    orderId: z.number(),
    userId: z.string(),
    userEmail: z.string().email(),
    userName: z.string(),
    orderDescription: z.string(),
    status: z.string(),
    totalAmount: z.number(),
    createdAt: z.string(),
    // Exclude: sagaId (internal workflow tracking)
});



// BE Response wrapper schemas
const OrderDetailResponseSchema = z.object({
    status: z.number(),
    msg: z.string(),
    data: OrderDetailSchema,
});



export {
    OrderDetailSchema,
    OrderDetailResponseSchema
};