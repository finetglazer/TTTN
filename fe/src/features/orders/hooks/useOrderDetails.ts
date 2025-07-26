// src/hooks/useOrderDetails.ts
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

    // Status polling hooks
    const { data: orderStatus } = useOrderStatusPolling(orderId);
    const { data: paymentStatus } = usePaymentStatusPolling(orderId);

    // Invalidate main queries when status changes =>
    /*
    * This is no need now because we do not update any information of order
    *
     */
    // useEffect(() => {
    //     if (orderStatus && orderQuery.data?.status !== orderStatus) {
    //         console.log(`Order status changed to: ${orderStatus}`);
    //         queryClient.invalidateQueries({ queryKey: ordersKeys.detail(orderId) });
    //     }
    // }, [orderStatus, orderQuery.data?.status, queryClient, orderId]);

    useEffect(() => {
        if (paymentStatus && paymentQuery.data?.status !== paymentStatus) {
            console.log(`Payment status changed to: ${paymentStatus}`);
            queryClient.invalidateQueries({ queryKey: paymentsKeys.byOrder(orderId) });
        }
    }, [paymentStatus, paymentQuery.data?.status, queryClient, orderId]);

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
        // Expose individual states if needed
        orderQuery,
        paymentQuery,
    };
};