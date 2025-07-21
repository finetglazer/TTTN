// import { useQuery } from '@tanstack/react-query';
// // import { orderService } from '@/services/orderService';
// import { TIMING } from '@/core/config/constants';
//
// export const useOrders = (filters?: OrderFilters) => {
//     return useQuery({
//         queryKey: ['orders', filters],
//         queryFn: () => orderService.getOrders(filters),
//         staleTime: TIMING.QUERY_STALE_TIME,
//     });
// };
//
// export const useOrder = (id: string) => {
//     return useQuery({
//         queryKey: ['order', id],
//         queryFn: () => orderService.getOrder(id),
//         enabled: !!id,
//     });
// };