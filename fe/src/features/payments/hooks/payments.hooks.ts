// fe/src/features/payments/hooks/payments.hooks.ts
import { useQuery } from '@tanstack/react-query';
import { paymentsApi } from '../api/payments.api';
import {PAYMENT, paymentsKeys} from "@/core/config/constants";

// ✅ FIX: Add enabled parameter to control when polling starts
export const usePaymentStatusPolling = (orderId: string, enabled: boolean = true) => {
    return useQuery({
        queryKey: paymentsKeys.status(orderId),
        queryFn: () => paymentsApi.getPaymentStatusByOrder(orderId),

        // Polling configuration
        refetchInterval: (query) => {
            // Terminal states - stop polling
            if (query.state.data === PAYMENT.STATUS.CONFIRMED ||
                query.state.data === PAYMENT.STATUS.FAILED ||
                query.state.data === PAYMENT.STATUS.DECLINED) {
                return false;
            }

            // Active states - more frequent polling
            if (query.state.data === PAYMENT.STATUS.PENDING) {
                return 5 * 1000;
            }
            return 10 * 60 * 1000; // 10 minutes for other states (reduced from 10 minutes)
        },
        refetchIntervalInBackground: false,

        // Cache configuration
        staleTime: (query) => {
            const status = query.state.data;

            // Terminal states: NEVER refetch
            if (status === PAYMENT.STATUS.CONFIRMED ||
                status === PAYMENT.STATUS.FAILED ||
                status === PAYMENT.STATUS.DECLINED) {
                return Infinity; // ✨ Endless cache!
            }

            return 0;
        },
        gcTime: Infinity,

        // ✅ FIX: Only start polling if orderId exists AND enabled (payment data exists)
        enabled: !!orderId && enabled,

        // ✅ FIX: Handle payment not found gracefully
        retry: (failureCount, error) => {
            // Don't retry if payment transaction doesn't exist yet
            if (error?.message?.includes('Payment transaction not found')) {
                return false;
            }
            return failureCount < 3;
        },

        networkMode: 'offlineFirst',
    });
};