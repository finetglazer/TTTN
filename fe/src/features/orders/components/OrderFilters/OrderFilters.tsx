// fe/src/features/orders/components/OrdersFilters/OrdersFilters.tsx
'use client';

import { FILTER_OPTIONS } from '@/core/config/constants';

// Types for filter state
export interface OrdersFilterState {
    searchTerm: string;
    statusFilter: string;
}

interface OrdersFiltersProps {
    filterState: OrdersFilterState;
    onFilterChange: (filterState: OrdersFilterState) => void;
    totalOrders: number;
    filteredCount: number;
}

// Sub-component for search input
function SearchInput({
                         searchTerm,
                         onSearchChange
                     }: {
    searchTerm: string;
    onSearchChange: (value: string) => void;
}) {
    return (
        <div className="flex-1 max-w-md">
            <input
                type="text"
                placeholder="Search by order ID, customer, or description..."
                className="form-input"
                value={searchTerm}
                onChange={(e) => onSearchChange(e.target.value)}
            />
        </div>
    );
}

// Sub-component for status filter
function StatusFilter({
                          statusFilter,
                          onStatusChange
                      }: {
    statusFilter: string;
    onStatusChange: (value: string) => void;
}) {
    return (
        <select
            className="form-input"
            value={statusFilter}
            onChange={(e) => onStatusChange(e.target.value)}
        >
            {FILTER_OPTIONS.map(option => (
                <option key={option.value} value={option.value}>
                    {option.label}
                </option>
            ))}
        </select>
    );
}

// Sub-component for filter actions
function FilterActions({
                           onClearFilters,
                           hasActiveFilters
                       }: {
    onClearFilters: () => void;
    hasActiveFilters: boolean;
}) {
    return (
        <button
            onClick={onClearFilters}
            className={`btn-ghost ${!hasActiveFilters ? 'opacity-50 cursor-not-allowed' : ''}`}
            disabled={!hasActiveFilters}
        >
            Clear Filters
        </button>
    );
}

// Sub-component for filter results summary
function FilterSummary({
                           totalOrders,
                           filteredCount,
                           hasActiveFilters
                       }: {
    totalOrders: number;
    filteredCount: number;
    hasActiveFilters: boolean;
}) {
    if (!hasActiveFilters) return null;

    return (
        <div className="text-sm text-[#718096]">
            Showing {filteredCount} of {totalOrders} orders
        </div>
    );
}

// Main OrdersFilters component
export default function OrdersFilters({
                                          filterState,
                                          onFilterChange,
                                          totalOrders,
                                          filteredCount
                                      }: OrdersFiltersProps) {

    const handleSearchChange = (searchTerm: string) => {
        onFilterChange({
            ...filterState,
            searchTerm
        });
    };

    const handleStatusChange = (statusFilter: string) => {
        onFilterChange({
            ...filterState,
            statusFilter
        });
    };

    const handleClearFilters = () => {
        onFilterChange({
            searchTerm: '',
            statusFilter: 'all'
        });
    };

    // Check if any filters are active
    const hasActiveFilters = filterState.searchTerm !== '' || filterState.statusFilter !== 'all';

    return (
        <div className="card mb-8">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <SearchInput
                    searchTerm={filterState.searchTerm}
                    onSearchChange={handleSearchChange}
                />

                <div className="flex items-center gap-4">
                    <StatusFilter
                        statusFilter={filterState.statusFilter}
                        onStatusChange={handleStatusChange}
                    />

                    <FilterActions
                        onClearFilters={handleClearFilters}
                        hasActiveFilters={hasActiveFilters}
                    />
                </div>
            </div>

            {/* Filter Summary */}
            <FilterSummary
                totalOrders={totalOrders}
                filteredCount={filteredCount}
                hasActiveFilters={hasActiveFilters}
            />
        </div>
    );
}

// Export filter logic utility
export const filterOrders = (orders: any[], filterState: OrdersFilterState) => {
    return orders.filter(order => {
        const matchesSearch = filterState.searchTerm === '' ||
            order.orderId.toLowerCase().includes(filterState.searchTerm.toLowerCase()) ||
            order.userName.toLowerCase().includes(filterState.searchTerm.toLowerCase()) ||
            order.orderDescription.toLowerCase().includes(filterState.searchTerm.toLowerCase());

        const matchesStatus = filterState.statusFilter === 'all' ||
            order.orderStatus === filterState.statusFilter;

        return matchesSearch && matchesStatus;
    });
};