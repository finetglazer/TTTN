import { z } from 'zod';

// Updated schema to match CreateOrderRequest interface directly
const createOrderRequestSchema = z.object({
    userId: z.string().min(1, 'User ID is required'),

    userEmail: z
        .string()
        .min(1, 'Email is required')
        .email('Invalid email format'),

    userName: z.string().min(1, 'User name is required'),

    orderDescription: z
        .string()
        .min(1, 'Order description is required')
        .max(999, 'Order description must be less than 1000 characters'),

    totalAmount: z
        .number()
        .positive('Total amount must be positive')
        .max(9999999999.99, 'Total amount is too large'),

    shippingAddress: z
        .string()
        .min(1, 'Shipping address is required')
        .max(999, 'Shipping address must be less than 1000 characters'),
});

// Updated response schema to match actual backend response
const createOrderResponseSchema = z.object({
    status: z.number(),
    msg: z.string(),
    data: z.object({
        orderId: z.number(),
        userId: z.string(),
        userEmail: z.string(),
        userName: z.string(),
        orderDescription: z.string(),
        totalAmount: z.number(),
        status: z.string(),
        createdAt: z.string(),
        sagaId: z.string().nullable(),
    }),
});

export type CreateOrderResponse = z.infer<typeof createOrderResponseSchema>;

// Keep the old formSchema for backward compatibility if needed elsewhere
const formSchema = z.object({
    userId: z.string().min(1, 'User ID is required'),

    userEmail: z
        .string()
        .min(1, 'Email is required')
        .email('Invalid email format'),

    name: z.string().min(1, 'Name is required'),

    description: z
        .string()
        .min(1, 'Description is required')
        .max(999, 'Description must be less than 1000 characters'),

    amount: z
        .string()
        .min(1, 'Amount is required')
        .regex(
            /^\d{1,10}\.\d{2}$/,
            'Amount must have at most 10 integer digits and exactly 2 decimal places'
        ),

    shippingAddress: z
        .string()
        .min(1, 'Shipping address is required')
        .max(999, 'Shipping address must be less than 1000 characters'),
});

export {
    formSchema,
    createOrderRequestSchema,
    createOrderResponseSchema
}