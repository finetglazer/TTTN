'use client';

import { useFetchAllOrders } from '@/features/orders/hooks/orders.hooks';
import OrderDashboard from '@/features/orders/components/OrdersDashboard';

export default function DashboardPage() {
    const {
        data: orders = [],
        isLoading,
        error,
        isError
    } = useFetchAllOrders();

    return (
        <OrderDashboard
            orders={orders}
            isLoading={isLoading}
            error={isError ? error : null}
        />
    );
}