// fe/src/features/orders/components/OrderTable/OrderTableActions.tsx
'use client';

import { useRouter } from 'next/navigation';
import { ORDER } from '@/core/config/constants';
import { OrdersDashboardDisplay } from '@/features/orders/types/orders.dashboard.types';

interface OrderTableActionsProps {
    order: OrdersDashboardDisplay;
    onCancel?: (orderId: string) => void;
}

export function OrderTableActions({ order, onCancel }: OrderTableActionsProps) {
    const router = useRouter();

    const handleViewDetails = () => {
        router.push(`/orders/${order.orderId}`);
    };

    const handleCancel = () => {
        if (onCancel) {
            onCancel(order.orderId);
        }
    };

    return (
        <div className="flex items-center space-x-2">
            <button
                onClick={handleViewDetails}
                className="text-[#f6d55c] hover:text-[#e6c53f] border border-[#f6d55c] hover:border-[#e6c53f] px-3 py-1 rounded text-sm font-medium transition-colors duration-200"
            >
                View Details
            </button>
            {order.orderStatus === ORDER.STATUS.CREATED && (
                <button
                    onClick={handleCancel}
                    className="text-red-600 hover:text-red-800 text-sm"
                >
                    Cancel
                </button>
            )}
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
            case ORDER.STATUS.CREATED:
                return baseClass + 'bg-blue-100 text-blue-800';
            case ORDER.STATUS.CONFIRMED:
                return baseClass + 'bg-orange-100 text-orange-800';
            case ORDER.STATUS.DELIVERED:
                return baseClass + 'bg-green-100 text-green-800';
            case ORDER.STATUS.CANCELLED:
                return baseClass + 'bg-red-100 text-red-800';
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
            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-[#1a1a1a]">
                #{order.orderId}
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1a1a]">
                {order.userName}
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-[#1a1a1a]">
                ${order.totalAmount.toFixed(2)}
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <span className={getStatusBadgeClass(order.orderStatus)}>
                    {order.orderStatus}
                </span>
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-[#718096]">
                {new Date(order.createdAt).toLocaleDateString()}
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <OrderTableActions order={order} />
            </td>
        </tr>
    );
}