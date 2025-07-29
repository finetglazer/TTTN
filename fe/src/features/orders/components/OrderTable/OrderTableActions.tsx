// fe/src/features/orders/components/OrderTable/OrderTableActions.tsx
'use client';

import { useRouter } from 'next/navigation';
import { OrdersDashboardDisplay } from '@/features/orders/types/orders.dashboard.types';
import { CancelOrderButtonSmall } from '@/features/orders/components/CancelOrderButton';

interface OrderTableActionsProps {
    order: OrdersDashboardDisplay;
}

export function OrderTableActions({ order }: OrderTableActionsProps) {
    const router = useRouter();

    const handleViewDetails = () => {
        router.push(`/orders/${order.orderId}`);
    };

    return (
        <div className="flex items-center space-x-2">
            <button
                onClick={handleViewDetails}
                className="btn-ghost inline-flex items-center text-sm px-3 py-1 rounded-md hover:bg-gray-100 transition-colors duration-200"
            >
                View Details
            </button>

            {/* Use the new CancelOrderButton component */}
            <CancelOrderButtonSmall
                orderId={order.orderId}
                orderStatus={order.orderStatus}
            />
        </div>
    );
}

// Updated OrderRow component for OrderTable
export function OrderRowWithNavigation({
                                           order,
                                           index
                                       }: {
    order: OrdersDashboardDisplay;
    index: number;
}) {
    const getStatusBadgeClass = (status: string) => {
        const baseClass = 'px-2 py-1 rounded-full text-xs font-medium ';
        switch (status) {
            case 'CREATED':
                return baseClass + 'bg-blue-100 text-blue-800';
            case 'CONFIRMED':
                return baseClass + 'bg-orange-100 text-orange-800';
            case 'DELIVERED':
                return baseClass + 'bg-green-100 text-green-800';
            case 'CANCELLED':
                return baseClass + 'bg-red-100 text-red-800';
            case 'CANCELLATION_PENDING':
                return baseClass + 'bg-yellow-100 text-yellow-800';
            default:
                return baseClass + 'bg-gray-100 text-gray-800';
        }
    };

    return (
        <tr
            key={order.orderId}
            className={`hover:bg-gray-50 transition-colors duration-150 ${
                index % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'
            }`}
        >
            <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm font-medium text-gray-900">
                    #{order.orderId}
                </div>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-900">{order.userName}</div>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm font-medium text-gray-900">
                    ${order.totalAmount.toFixed(2)}
                </div>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <span className={getStatusBadgeClass(order.orderStatus)}>
                    {order.orderStatus.replace('_', ' ')}
                </span>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-500">
                    {new Date(order.createdAt).toLocaleDateString()}
                </div>
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <OrderTableActions order={order} />
            </td>
        </tr>
    );
}