import { z } from 'zod';

export const formSchema = z.object({
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

export type FormData = z.infer<typeof formSchema>;