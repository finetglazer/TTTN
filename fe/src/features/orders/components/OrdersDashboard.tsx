// fe/src/features/orders/components/OrdersDashboard.tsx (REFACTORED)
'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import { OrdersDashboardDisplay } from '@/features/orders/types/orders.dashboard.types';

// Import the new divided components
import OrdersFilters, { OrdersFilterState, filterOrders } from '@/features/orders/components/OrderFilters/OrderFilters';
import OrdersTable from '@/features/orders/components/OrderTable/OrderTable';

interface OrderDashboardProps {
    orders: OrdersDashboardDisplay[];
    isLoading?: boolean;
    error?: Error | null;
}

export default function OrderDashboard({
                                           orders,
                                           isLoading = false,
                                           error = null
                                       }: OrderDashboardProps) {
    // Filter state management
    const [filterState, setFilterState] = useState<OrdersFilterState>({
        searchTerm: '',
        statusFilter: 'all'
    });

    // Apply filters to orders
    const filteredOrders = useMemo(() => {
        return filterOrders(orders, filterState);
    }, [orders, filterState]);

    // Handle filter state changes
    const handleFilterChange = (newFilterState: OrdersFilterState) => {
        setFilterState(newFilterState);
    };

    return (
        <div className="min-h-screen bg-[#f7fafc]">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Page Header */}
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-[#1a1a1a] mb-2">
                            Orders Dashboard
                        </h1>
                        <p className="text-[#718096]">
                            Manage and track all your orders
                        </p>
                    </div>
                    <Link href="/create-order" className="btn-primary">
                        Create New Order
                    </Link>
                </div>

                {/* Filters Section */}
                <OrdersFilters
                    filterState={filterState}
                    onFilterChange={handleFilterChange}
                    totalOrders={orders.length}
                    filteredCount={filteredOrders.length}
                />

                {/* Orders Table Section */}
                <OrdersTable
                    orders={filteredOrders}
                    isLoading={isLoading}
                    error={error}
                />
            </div>
        </div>
    );
}