import { axiosClient } from '@/core/config/axios-client';
import { CreateOrderRequest } from '../types/orders.create.types';
import {
    createOrderRequestSchema,
    CreateOrderResponse,
    createOrderResponseSchema
} from '@/features/orders/validations/orders.schema';
import {API} from '@/core/config/constants';
import {GetAllOrdersResponse, OrdersDashboardDisplay} from "@/features/orders/types/orders.dashboard.types";
import {getAllOrdersResponseSchema} from "@/features/orders/validations/orders.status.schema";
import {OrderDetail} from "@/features/orders/types/orders.detail.types";
import {OrderDetailResponseSchema} from "@/features/orders/validations/orderDetail.schema";

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
    // GET - Fetch order status by order ID
    getOrderStatus: async (orderId: string): Promise<string> => {
        const response = await axiosClient.get(`${API.ENDPOINTS.ORDERS.BASE_URL}/${orderId}/status`);
        return response.data.data.status;
    },
    // GET - Fetch order details by order ID
    getOrderById: async (orderId: string): Promise<OrderDetail> => {
        const response = await axiosClient.get(`${API.ENDPOINTS.ORDERS.BASE_URL}/${orderId}`);

        // Parse and validate response
        const parsedResponse = OrderDetailResponseSchema.parse(response.data);
        return parsedResponse.data;
    }
};