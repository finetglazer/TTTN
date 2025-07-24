import { axiosClient } from '@/core/config/axios-client';
import {Order, CreateOrderRequest} from '../types/orders.create.types';
import { formSchema } from '@/features/orders/validations/orders.schema';
import {API, ROUTES} from '@/core/config/constants';
import {GetAllOrdersResponse, OrdersDashboardDisplay} from "@/features/orders/types/orders.dashboard.types";

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
        const { data } = await axiosClient.post<Order>(API.ENDPOINTS.ORDERS.CREATE, orderData);
        return data;
    },

    // GET - Fetch all orders
    getAllOrders: async (): Promise<OrdersDashboardDisplay[]> => {
        const { data } = await axiosClient.get<GetAllOrdersResponse>(API.ENDPOINTS.ORDERS.LIST);
        return data.data;
    },
};