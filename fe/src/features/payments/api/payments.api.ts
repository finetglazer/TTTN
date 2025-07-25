// fe/src/features/payments/api/payments.api.ts
import {axiosClient} from "@/core/config/axios-client";
import {API} from "@/core/config/constants";
import {PaymentDetail} from "@/features/payments/types/payment.detail.types";
import {PaymentDetailResponseSchema} from "@/features/payments/validation/paymentDetails.schema";

export const paymentsApi = {
    getPaymentStatusByOrder: async (orderId: string): Promise<string> => {
        const response = await axiosClient.get(`${API.ENDPOINTS.PAYMENTS.BASE_URL}/order/${orderId}/status`);
        return response.data.data.status;
    },

    getPaymentByOrderId: async (orderId: string): Promise<PaymentDetail | null> => {
        const response = await axiosClient.get(`${API.ENDPOINTS.PAYMENTS.BASE_URL}/order/${orderId}`);

        // Parse and validate response
        const parsedResponse = PaymentDetailResponseSchema.parse(response.data);
        return parsedResponse.data;
    }
}