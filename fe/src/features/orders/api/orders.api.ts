import { axiosClient } from '@/core/config/axios-client';
import {Order, CreateOrderRequest} from '../types/orders.create.types';
import {
    createOrderRequestSchema,
    CreateOrderResponse,
    createOrderResponseSchema,
    formSchema
} from '@/features/orders/validations/orders.schema';
import {API, ROUTES} from '@/core/config/constants';
import {GetAllOrdersResponse, OrdersDashboardDisplay} from "@/features/orders/types/orders.dashboard.types";
import {getAllOrdersResponseSchema} from "@/features/orders/validations/orders.status.schema";

export const ordersApi = {
    // POST - Create new order
    createOrder: async (orderData: CreateOrderRequest): Promise<CreateOrderResponse> => {
        // Validate input data using Zod schema
        const validatedData = createOrderRequestSchema.parse(orderData);

        // Send API request with the original structure expected by backend
        const { data } = await axiosClient.post<CreateOrderResponse>(API.ENDPOINTS.ORDERS.CREATE, validatedData);
        return createOrderResponseSchema.parse(data);
    },

    // GET - Fetch all orders
    getAllOrders: async (): Promise<OrdersDashboardDisplay[]> => {
        const { data } = await axiosClient.get<GetAllOrdersResponse>(API.ENDPOINTS.ORDERS.LIST);
        const validated = getAllOrdersResponseSchema.parse(data);
        return validated.data;
    },
};