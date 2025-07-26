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
        <div className="min-h-screen bg-[#f7fafc] page-entrance">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Page Header with staggered animation */}
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-[#1a1a1a] mb-2 header-stagger-1">
                            Orders Dashboard
                        </h1>
                        <p className="text-[#718096] header-stagger-2">
                            Manage and track all your orders
                        </p>
                    </div>
                    <Link href="/create-order" className="btn-primary header-stagger-3">
                        Create New Order
                    </Link>
                </div>

                {/* Filters Section with slide-in animation */}
                <div className="filters-slide-in">
                    <OrdersFilters
                        filterState={filterState}
                        onFilterChange={handleFilterChange}
                        totalOrders={orders.length}
                        filteredCount={filteredOrders.length}
                    />
                </div>

                {/* Orders Table Section with slide-up animation */}
                <div className="content-slide-up">
                    <OrdersTable
                        orders={filteredOrders}
                        isLoading={isLoading}
                        error={error}
                    />
                </div>
            </div>
        </div>
    );
    }