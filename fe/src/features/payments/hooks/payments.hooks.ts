
import { useQuery } from '@tanstack/react-query';
import { paymentsApi } from '../api/payments.api';
import {PAYMENT, paymentsKeys} from "@/core/config/constants";


export const usePaymentStatusPolling = (orderId: string) => {
    return useQuery({
        queryKey: paymentsKeys.status(orderId),
        queryFn: () => paymentsApi.getPaymentStatusByOrder(orderId),

        // Polling configuration
        refetchInterval: (query) => {
            // Terminal states - stop polling
            if (query.state.data === PAYMENT.STATUS.CONFIRMED || query.state.data === PAYMENT.STATUS.FAILED || query.state.data === PAYMENT.STATUS.DECLINED) {
                return false;
            }

            // Active states - more frequent polling
            if (query.state.data === PAYMENT.STATUS.PENDING) {
                return 5 * 1000;
            }
            return 10 * 60 * 1000; // 10 minutes for other states
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

        // Only start polling if orderId exists
        enabled: !!orderId,

        networkMode: 'offlineFirst',
    });
};

