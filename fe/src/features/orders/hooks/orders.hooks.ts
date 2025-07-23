import {useMutation, useQueryClient} from "@tanstack/react-query";
import {ordersApi} from "@/features/orders/api/orders.api";
import {CreateOrderRequest} from "@/features/orders/types/orders.types";

// Mutation hook for creating a new order
export const useCreateOrder = () => {
  const queryClient = useQueryClient();

  return useMutation({
      mutationFn: (orderData: CreateOrderRequest) => ordersApi.createOrder(orderData),
    //   onSuccess: () {
    //       // queryClient.invalidateQueries({queryKey: ordersKeys.list()});
    //
    // },
    onError: (error) => {
          console.error('Failed to create order:', error);
    },
  });
};