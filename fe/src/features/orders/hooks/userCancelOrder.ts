// fe/src/features/orders/hooks/useCancelOrder.ts
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { ordersApi } from '@/features/orders/api/orders.api';
import { ordersKeys, ORDER } from '@/core/config/constants';

interface CancelOrderState {
    isLoading: boolean;
    notification: {
        isOpen: boolean;
        message: string;
        isSuccess: boolean;
        orderId?: string;
        allowRetry: boolean;
    };
}

interface CancelOrderResponse {
    success: boolean;
    message: string;
    status: number;
    data?: any;
}

export const useCancelOrder = () => {
    const queryClient = useQueryClient();
    const [state, setState] = useState<CancelOrderState>({
        isLoading: false,
        notification: {
            isOpen: false,
            message: '',
            isSuccess: false,
            allowRetry: false,
        }
    });

    const cancelOrder = async (orderId: string, reason?: string) => {
        // Check if button is already in loading state
        if (state.isLoading) return;

        setState(prev => ({
            ...prev,
            isLoading: true,
            notification: { ...prev.notification, isOpen: false }
        }));

        try {
            // Call the API
            const response = await ordersApi.cancelOrder(orderId, reason);

            // The API returns { success: boolean, message: string }
            // But we need to handle the actual backend response format
            const { success, message } = response;

            if (success) {
                // Success case: Order cancellation initiated
                setState(prev => ({
                    ...prev,
                    isLoading: false,
                    notification: {
                        isOpen: true,
                        message: "Order cancellation has been initiated successfully. The order status will be updated shortly.",
                        isSuccess: true,
                        orderId: orderId,
                        allowRetry: false,
                    }
                }));

                // Invalidate queries to refresh order data
                queryClient.invalidateQueries({ queryKey: ordersKeys.list() });
                queryClient.invalidateQueries({ queryKey: ordersKeys.detail(orderId) });
                queryClient.invalidateQueries({ queryKey: ordersKeys.status(orderId) });

            } else {
                // Handle failure cases based on message content
                let notificationMessage = message;
                let allowRetry = true;

                // Check for specific failure scenarios
                if (message.includes('Order status changed while processing') ||
                    message.includes('Payment is being processed')) {
                    notificationMessage = "The order status changed while processing cancellation. Please refresh and try again.";
                    allowRetry = true;
                } else if (message.includes('Cannot cancel order that has already been') ||
                    message.includes('Cancellation not allowed')) {
                    notificationMessage = "This order cannot be cancelled as it has already been processed or delivered.";
                    allowRetry = false;
                } else {
                    notificationMessage = message || "Failed to cancel order. Please try again.";
                    allowRetry = true;
                }

                setState(prev => ({
                    ...prev,
                    isLoading: false,
                    notification: {
                        isOpen: true,
                        message: notificationMessage,
                        isSuccess: false,
                        orderId: orderId,
                        allowRetry: allowRetry,
                    }
                }));
            }

        } catch (error: any) {
            // Network or other errors
            console.error('Cancel order error:', error);

            let errorMessage = "Network error occurred. Please check your connection and try again.";

            // Handle specific error cases
            if (error.response?.status === 404) {
                errorMessage = "Order not found. Please refresh the page.";
            } else if (error.response?.status >= 500) {
                errorMessage = "Server error occurred. Please try again later.";
            } else if (error.message) {
                errorMessage = error.message;
            }

            setState(prev => ({
                ...prev,
                isLoading: false,
                notification: {
                    isOpen: true,
                    message: errorMessage,
                    isSuccess: false,
                    orderId: orderId,
                    allowRetry: true,
                }
            }));
        }
    };

    const closeNotification = () => {
        setState(prev => ({
            ...prev,
            notification: { ...prev.notification, isOpen: false }
        }));
    };

    const retryCancel = (orderId: string, reason?: string) => {
        cancelOrder(orderId, reason);
    };

    // Check if order can be cancelled based on status
    const canCancelOrder = (orderStatus: string): boolean => {
        return orderStatus === ORDER.STATUS.CREATED ||
            orderStatus === ORDER.STATUS.CONFIRMED;
    };

    return {
        cancelOrder,
        closeNotification,
        retryCancel,
        canCancelOrder,
        isLoading: state.isLoading,
        notification: state.notification,
    };
};