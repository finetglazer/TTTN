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

    // ✅ FIX 1: Dynamic staleTime and refetch for payment query
    const paymentQuery = useQuery({
        queryKey: paymentsKeys.byOrder(orderId),
        queryFn: () => paymentsApi.getPaymentByOrderId(orderId),

        // 🎯 Dynamic staleTime based on payment existence
        staleTime: (query) => {
            // If no payment data exists, keep checking frequently
            if (!query.state.data) {
                return 0; // Always fresh when no payment data
            }
            return 10 * 60 * 1000; // 10 minutes when payment exists
        },

        gcTime: 60 * 60 * 1000, // 1 hour
        enabled: !!orderId,

        // ✅ FIX 2: Add refetch interval when payment is null (CREATED orders)
        refetchInterval: (query) => {
            // If no payment data exists, keep polling every 5 seconds
            if (!query.state.data) {
                return 5 * 1000; // 5 seconds
            }
            // If payment exists, stop interval polling (rely on status polling)
            return false;
        },

        // Don't treat payment not found as error for CREATED orders
        retry: (failureCount, error) => {
            // Don't retry if it's a payment not found scenario
            if (error?.message?.includes('Payment transaction not found')) {
                return false;
            }
            return failureCount < 3;
        }
    });

    // ✅ FIX 3: Only poll payment status if payment data exists
    const { data: liveOrderStatus, isLoading: orderStatusLoading } = useOrderStatusPolling(orderId);
    const { data: livePaymentStatus, isLoading: paymentStatusLoading } = usePaymentStatusPolling(
        orderId,
        !!paymentQuery.data // Only poll if payment exists
    );

    // ✅ FIX 4: Enhanced invalidation logic
    useEffect(() => {
        if (livePaymentStatus && paymentQuery.data?.status !== livePaymentStatus) {
            console.log(`Payment status changed to: ${livePaymentStatus}`);
            queryClient.invalidateQueries({ queryKey: paymentsKeys.byOrder(orderId) });
        }
    }, [livePaymentStatus, paymentQuery.data?.status, queryClient, orderId]);

    // ✅ FIX 5: Detect when payment gets created for the first time
    // useEffect(() => {
    //     // If we suddenly have payment data when we didn't before, invalidate related queries
    //     if (paymentQuery.data && !paymentQuery.isPreviousData) {
    //         console.log('Payment transaction created! Refreshing all related data...');
    //         queryClient.invalidateQueries({ queryKey: ordersKeys.detail(orderId) });
    //         queryClient.invalidateQueries({ queryKey: paymentsKeys.status(orderId) });
    //     }
    // }, [paymentQuery.data, paymentQuery.isPreviousData, queryClient, orderId]);

    // ✅ FIX 6: Only treat as error if order fails, not payment
    const isLoading = orderQuery.isLoading || paymentQuery.isLoading;
    const isError = orderQuery.isError; // Only fail if order fails, not payment
    const error = orderQuery.error;

    // ✅ FIX 7: Combined data - allow null payment for CREATED orders
    const data: OrderDetailsData | undefined =
        orderQuery.data
            ? {
                order: orderQuery.data,
                payment: paymentQuery.data || null, // Allow null payment
            }
            : undefined;

    // Success state - order must succeed, payment is optional
    const isSuccess = orderQuery.isSuccess;

    return {
        data,
        isLoading,
        isError,
        isSuccess,
        error,

        // 🎯 Expose live polling data
        liveOrderStatus,      // Live order status from polling
        livePaymentStatus,    // Live payment status from polling (only when payment exists)
        statusLoading: orderStatusLoading || paymentStatusLoading,

        // Expose individual states if needed
        orderQuery,
        paymentQuery,

        // ✅ Expose payment loading state for UI
        isPaymentLoading: paymentQuery.isLoading,
        isPaymentError: paymentQuery.isError,
        hasPaymentData: !!paymentQuery.data,
    };
};