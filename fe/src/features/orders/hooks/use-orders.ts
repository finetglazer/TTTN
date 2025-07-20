// import { useQuery } from '@tanstack/react-query';
// // import { orderService } from '@/services/orderService';
//
// export const useOrders = (filters?: OrderFilters) => {
//     return useQuery({
//         queryKey: ['orders', filters],
//         queryFn: () => orderService.getOrders(filters),
//         staleTime: 30000, // 30 seconds
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