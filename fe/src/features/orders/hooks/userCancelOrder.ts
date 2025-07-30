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

interface CancelOrderApiResponse { // Renamed to avoid confusion with the internal response object
    success: boolean;
    message: string;
    status: number;
    data?: object; // Use unknown instead of any for better type safety
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
            const { status, msg, data } = response;

            // Check if this is a success case (either status === 1 OR message indicates success)
            const isSuccess = status === 1 || msg.includes('Order cancellation initiated');

            if (isSuccess) {
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

                // Delay query invalidation to allow notification to display properly
                setTimeout(() => {
                    queryClient.invalidateQueries({ queryKey: ordersKeys.list() });
                    queryClient.invalidateQueries({ queryKey: ordersKeys.detail(orderId) });
                    queryClient.invalidateQueries({ queryKey: ordersKeys.status(orderId) });
                }, 10000); // Small delay to prevent immediate re-render
            } else {
                // Handle failure cases based on message content
                let notificationMessage = msg;
                let allowRetry = true;

                // Check for specific failure scenarios
                if (msg.includes('Order status changed while processing') ||
                    msg.includes('Payment is currently being processed. Please wait for payment completion before cancelling.')) {
                    notificationMessage = "Payment is currently being processed. Please wait for payment completion before cancelling.";
                    allowRetry = true;
                } else if (msg.includes('Cannot cancel order that has already been') ||
                    msg.includes('Cancellation not allowed')) {
                    notificationMessage = "This order cannot be cancelled as it has already been processed or delivered.";
                    allowRetry = false;
                } else {
                    notificationMessage = msg || "Failed to cancel order. Please try again.";
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

        } catch (error: unknown) { // Use unknown for caught errors
            // Network or other errors
            console.error('Cancel order error:', error);

            let errorMessage = "Network error occurred. Please check your connection and try again.";

            // Refine error handling for 'error'
            if (error && typeof error === 'object' && 'response' in error && error.response && typeof error.response === 'object' && 'status' in error.response) {
                const responseError = error.response as { status?: number }; // Type assertion for response
                if (responseError.status === 404) {
                    errorMessage = "Order not found. Please refresh the page.";
                } else if (responseError.status && responseError.status >= 500) {
                    errorMessage = "Server error occurred. Please try again later.";
                }
            } else if (error instanceof Error) { // Check if error is an instance of Error
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