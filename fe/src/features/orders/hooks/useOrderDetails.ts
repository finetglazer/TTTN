// fe/src/features/orders/hooks/useOrderDetails.ts
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { ordersApi } from '../api/orders.api';

import type { OrderDetailsData } from '../types/orders.detail.types';
import {ordersKeys, paymentsKeys} from "@/core/config/constants";
import {paymentsApi} from "@/features/payments/api/payments.api";
import {usePaymentStatusPolling} from "@/features/payments/hooks/payments.hooks";
import {useOrderStatusPolling} from "@/features/orders/hooks/orders.hooks";

export const useOrderDetails = (orderId: string) => {
    const queryClient = useQueryClient();

    // Main data queries with longer stale time
    const orderQuery = useQuery({
        queryKey: ordersKeys.detail(orderId),
        queryFn: () => ordersApi.getOrderById(orderId),
        staleTime: 10 * 60 * 1000, // 10 minutes - longer as discussed
        gcTime: 60 * 60 * 1000, // 1 hour
        enabled: !!orderId,
    });

    const paymentQuery = useQuery({
        queryKey: paymentsKeys.byOrder(orderId),
        queryFn: () => paymentsApi.getPaymentByOrderId(orderId),
        staleTime: 10 * 60 * 1000, // 10 minutes
        gcTime: 60 * 60 * 1000, // 1 hour
        enabled: !!orderId,
    });

    // Status polling hooks - NOW EXPOSE THESE TO THE COMPONENT
    const { data: liveOrderStatus, isLoading: orderStatusLoading } = useOrderStatusPolling(orderId);
    const { data: livePaymentStatus, isLoading: paymentStatusLoading } = usePaymentStatusPolling(orderId);

    // Invalidate main queries when status changes
    useEffect(() => {
        if (livePaymentStatus && paymentQuery.data?.status !== livePaymentStatus) {
            console.log(`Payment status changed to: ${livePaymentStatus}`);
            queryClient.invalidateQueries({ queryKey: paymentsKeys.byOrder(orderId) });
        }
    }, [livePaymentStatus, paymentQuery.data?.status, queryClient, orderId]);

    // Combine states for clean interface
    const isLoading = orderQuery.isLoading || paymentQuery.isLoading;
    const isError = orderQuery.isError || paymentQuery.isError;
    const error = orderQuery.error || paymentQuery.error;

    // Combined data
    const data: OrderDetailsData | undefined =
        orderQuery.data && paymentQuery.data
            ? {
                order: orderQuery.data,
                payment: paymentQuery.data,
            }
            : undefined;

    // Success state - both queries must succeed
    const isSuccess = orderQuery.isSuccess && paymentQuery.isSuccess;

    return {
        data,
        isLoading,
        isError,
        isSuccess,
        error,

        // 🎯 NEW: Expose live polling data
        liveOrderStatus,      // Live order status from polling
        livePaymentStatus,    // Live payment status from polling
        statusLoading: orderStatusLoading || paymentStatusLoading,

        // Expose individual states if needed
        orderQuery,
        paymentQuery,
    };
};