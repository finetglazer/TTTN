import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ordersApi } from "@/features/orders/api/orders.api";
import {CreateOrderRequest, OrdersDashboardDisplay} from "@/features/orders/types/orders.create.types";
import {useEffect} from "react";

// Query key factory for better organization
export const ordersKeys = {
    all: ['orders'] as const,
    list: () => [...ordersKeys.all, 'list'] as const,
    detail: (id: string) => [...ordersKeys.all, 'detail', id] as const,
};

// Mutation hook for creating a new order
export const useCreateOrder = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (orderData: CreateOrderRequest) => ordersApi.createOrder(orderData),
        onSuccess: () => {
            // Invalidate and refetch orders list after successful creation
            queryClient.invalidateQueries({ queryKey: ordersKeys.list() });

            // Optional: Show success notification here
            console.log('Order created successfully, refreshing orders list...');
        },
        onError: (error) => {
            console.error('Failed to create order:', error);
            // Optional: Show error notification here
        },
    });
};

// Fetch hook for fetching all orders with enhanced offline support
export const useFetchAllOrders = () => {
    const queryClient = useQueryClient();

    const queryResult = useQuery<OrdersDashboardDisplay[], Error>({
        queryKey: ordersKeys.list(),
        queryFn: () => ordersApi.getAllOrders(),

        // Cache and refresh configuration
        staleTime: 60 * 1000, // 1 minute - data is fresh for 1 minute
        gcTime: 60 * 60 * 1000, // 1 hour - keep in cache for 1 hour (replaces cacheTime in v5)

        // Refetch behavior
        refetchOnMount: true, // Refetch when component mounts
        refetchOnWindowFocus: true, // Refetch when window regains focus
        refetchOnReconnect: true, // Refetch when reconnecting to internet
        refetchInterval: 60 * 1000, // Auto-refetch every 1 minute when tab is active
        refetchIntervalInBackground: false, // Don't refetch when tab is in background

        // Network and retry configuration for offline support
        // FIX 1: The 'error' parameter here needs an explicit type.
        retry: (failureCount, error: unknown) => {
            // Don't retry if it's a 4xx error (client error)
            // Note: Added type assertion for safety when accessing 'status'
            if (error && typeof error === 'object' && 'status' in error) {
                const status = (error as { status: number }).status;
                if (status >= 400 && status < 500) {
                    return false;
                }
            }
            // Retry up to 3 times for network errors or 5xx errors
            return failureCount < 3;
        },
        retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000), // Exponential backoff

        // Enhanced offline support
        networkMode: 'offlineFirst', // Use cached data when offline, still try network
    });

    const { isSuccess, isError, data, error} = queryResult;

    useEffect(() => {
        if (isSuccess) {
            console.log(`Successfully fetched ${data.length} orders`);
        }

        if (isError) {
            console.error('Error fetching orders:', error);
            const cachedData = queryClient.getQueryData(ordersKeys.list());
            if (cachedData) {
                console.log('Using cached orders data due to fetch error');
            }
        }
    }, [isSuccess, isError, data, queryClient, ordersKeys]);

    return queryResult;

};

// Optional: Hook to get cached orders count for UI indicators
export const useCachedOrdersCount = () => {
    const queryClient = useQueryClient();
    const cachedData = queryClient.getQueryData(ordersKeys.list());
    return Array.isArray(cachedData) ? cachedData.length : 0;
};

// Optional: Hook to manually refetch orders (useful for pull-to-refresh)
export const useRefreshOrders = () => {
    const queryClient = useQueryClient();

    return () => {
        return queryClient.invalidateQueries({ queryKey: ordersKeys.list() });
    };
};