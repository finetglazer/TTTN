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
                className="btn-ghost inline-flex items-center"
            >
                View Details
            </button>
            {order.orderStatus === ORDER.STATUS.CREATED && (
                <button
                    onClick={handleCancel}
                    className="btn-secondary text-red-600 border-red-600 hover:bg-red-600 hover:text-white"
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
            {/* Fixed Order column - restore two-line structure */}
            <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex flex-col">
                    <span className="text-sm font-medium text-[#1a1a1a]">
                        {order.orderId}
                    </span>
                    <span className="text-xs text-[#718096] truncate max-w-xs">
                        {order.orderDescription}
                    </span>
                </div>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex flex-col">
                    <span className="text-sm font-medium text-[#2d3748]">
                        {order.userName}
                    </span>
                </div>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <span className="text-sm font-semibold text-[#1a1a1a]">
                    ${order.totalAmount.toFixed(2)}
                </span>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <span className={getStatusBadgeClass(order.orderStatus)}>
                    {order.orderStatus}
                </span>
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-[#718096]">
                {new Date(order.createdAt).toLocaleDateString()}
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <OrderTableActions order={order} />
            </td>
        </tr>
    );
}