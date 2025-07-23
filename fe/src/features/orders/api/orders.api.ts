import { axiosClient } from '@/core/config/axios-client';
import { Order, CreateOrderRequest } from '../types/orders.types';
import { formSchema } from '@/features/orders/validations/orders.schema';
import { ROUTES } from '@/core/config/constants';

export const ordersApi = {
    // POST - Create new order
    createOrder: async (orderData: CreateOrderRequest): Promise<Order> => {
        // Validate input data using Zod schema
        const validatedData = formSchema.parse({
            userId: orderData.userId, // Assuming userId maps to userEmail for validation
            userEmail: orderData.userEmail,
            name: orderData.userName,
            description: orderData.orderDescription,
            amount: orderData.totalAmount.toFixed(2), // Convert number to string with 2 decimals
            shippingAddress: orderData.shippingAddress,
        });

        // Send API request with the original structure expected by backend
        const { data } = await axiosClient.post<Order>(ROUTES.CREATE_ORDER, orderData);
        return data;
    },
};