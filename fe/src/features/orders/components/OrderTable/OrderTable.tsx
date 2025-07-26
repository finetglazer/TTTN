// fe/src/features/orders/components/OrdersTable/OrdersTable.tsx
'use client';

import { useState, useMemo } from 'react';
import { ORDER, COLORS } from '@/core/config/constants';
import {OrdersDashboardDisplay, OrdersTableProps} from '@/features/orders/types/orders.dashboard.types';
import {useRouter} from "next/navigation";

// Sub-component for status badge

function StatusBadge({ status }: { status: string }) {
    const statusClasses: { [key: string]: string } = {
        [ORDER.STATUS.CREATED]: 'status-created',
        [ORDER.STATUS.CONFIRMED]: 'status-confirmed',
        [ORDER.STATUS.DELIVERED]: 'status-delivered',
        [ORDER.STATUS.CANCELLED]: 'status-cancelled',
    };

    const badgeClass = statusClasses[status] || 'bg-gray-500 text-white';

    return (
        <span className={`status-badge ${badgeClass}`}>
            {status}
        </span>
    );
}

// Sub-component for order row actions
// Enhanced OrderRowActions component with navigation
function OrderRowActions({ order }: { order: OrdersDashboardDisplay }) {
    const router = useRouter();

    const handleViewDetails = () => {
        router.push(`/orders/${order.orderId}`);
    };

    const handleCancel = () => {
        // Add your cancel logic here
        console.log('Cancel order:', order.orderId);
    };

    return (
        <div className="flex items-center space-x-2">
            <button
                onClick={handleViewDetails}
                className="btn-ghost text-sm"
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

// Sub-component for individual order row
function OrderRow({ order, index }: { order: OrdersDashboardDisplay; index: number; }) {
    return (
        <tr className={`table-row-cascade cascade-delay-${Math.min(index + 1, 8)} table-row-hover ${index % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}>
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
                    {/*<span className="text-xs text-[#718096]">*/}
                    {/* {order.userEmail}*/}
                    {/*</span>*/}
                </div>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <span className="text-sm font-semibold text-[#1a1a1a]">
                    ${order.totalAmount.toFixed(2)}
                </span>
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <StatusBadge status={order.orderStatus} />
            </td>
            <td className="px-6 py-4 whitespace-nowrap text-sm text-[#718096]">
                {new Date(order.createdAt).toLocaleDateString()}
            </td>
            <td className="px-6 py-4 whitespace-nowrap">
                <OrderRowActions order={order} />
            </td>
        </tr>
    );
}

// Sub-component for table header
function TableHeader() {
    return (
        <thead className="bg-[#2d3748] text-white">
        <tr>
            <th className="px-6 py-4 text-left text-sm font-medium uppercase tracking-wider">
                Order
            </th>
            <th className="px-6 py-4 text-left text-sm font-medium uppercase tracking-wider">
                Customer
            </th>
            <th className="px-6 py-4 text-left text-sm font-medium uppercase tracking-wider">
                Amount
            </th>
            <th className="px-6 py-4 text-left text-sm font-medium uppercase tracking-wider">
                Status
            </th>
            <th className="px-6 py-4 text-left text-sm font-medium uppercase tracking-wider">
                Date
            </th>
            <th className="px-6 py-4 text-left text-sm font-medium uppercase tracking-wider">
                Actions
            </th>
        </tr>
        </thead>
    );
}

// Sub-component for pagination
function Pagination({
                        currentPage,
                        totalPages,
                        onPageChange
                    }: {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}) {
    if (totalPages <= 1) return null;

    return (
        <div className="flex items-center justify-center space-x-2 mt-6 pt-6 border-t">
            <button
                onClick={() => onPageChange(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
                className="pagination-btn btn-ghost disabled:opacity-50"
            >
                Previous
            </button>

            {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                <button
                    key={page}
                    onClick={() => onPageChange(page)}
                    className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                        currentPage === page
                            ? `text-black`
                            : 'text-[#718096] hover:bg-gray-100'
                    }`}
                    style={{
                        backgroundColor: currentPage === page ? COLORS.PRIMARY_GOLD : undefined,
                        color: currentPage === page ? COLORS.DEEP_CHARCOAL : undefined
                    }}
                >
                    {page}
                </button>
            ))}

            <button
                onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
                disabled={currentPage === totalPages}
                className="pagination-btn btn-ghost disabled:opacity-50"            >
                Next
            </button>
        </div>
    );
}

// Sub-component for empty state
function EmptyState({
                        hasOrders,
                        hasFiltered
                    }: {
    hasOrders: boolean;
    hasFiltered: boolean;
}) {
    return (
        <div className="text-center py-12">
            <div className="text-[#718096] text-lg mb-4">
                {!hasOrders
                    ? "No orders found"
                    : "No orders match your current filters"
                }
            </div>
            <p className="text-sm text-[#718096] mb-6">
                {!hasOrders
                    ? "Create your first order to get started"
                    : "Try adjusting your search or filter criteria"
                }
            </p>
        </div>
    );
}

// Sub-component for loading state
function LoadingState() {
    return (
        <div className="card">
            <div className="flex justify-center items-center py-12">
                <div className="loading-spinner rounded-full h-8 w-8 border-4 border-t-4 border-gray-200"></div>
                <span className="ml-3 text-[#718096]">Loading orders...</span>
            </div>
        </div>
    );
}

// Sub-component for error state
function ErrorState({
                        error,
                        onRetry
                    }: {
    error: Error;
    onRetry: () => void;
}) {
    return (
        <div className="card">
            <div className="text-center py-12">
                <div className="text-red-600 text-lg mb-4">Error loading orders</div>
                <p className="text-[#718096] mb-4">{error.message}</p>
                <button
                    onClick={onRetry}
                    className="btn-primary"
                >
                    Try Again
                </button>
            </div>
        </div>
    );
}

// Main OrdersTable component
export default function OrdersTable({
                                        orders,
                                        isLoading = false,
                                        error = null
                                    }: OrdersTableProps) {
    const [currentPage, setCurrentPage] = useState(1);
    const ordersPerPage = ORDER.PAGINATION.ORDERS_PER_PAGE;

    // Pagination calculations
    const totalPages = Math.ceil(orders.length / ordersPerPage);
    const startIndex = (currentPage - 1) * ordersPerPage;
    const paginatedOrders = orders.slice(startIndex, startIndex + ordersPerPage);

    // Reset pagination when orders change
    useMemo(() => {
        setCurrentPage(1);
    }, [orders.length]);

    const handleRetry = () => {
        window.location.reload();
    };

    // Loading state
    if (isLoading) {
        return <LoadingState />;
    }

    // Error state
    if (error) {
        return <ErrorState error={error} onRetry={handleRetry} />;
    }

    // Empty state
    if (orders.length === 0) {
        return <EmptyState hasOrders={false} hasFiltered={false} />;
    }

    return (
        <div className="card">
            <div>
                <table className="w-full">
                    <TableHeader />
                    <tbody className="bg-white divide-y divide-gray-200">
                    {paginatedOrders.map((order, index) => (
                        <OrderRow
                            key={order.orderId}
                            order={order}
                            index={index}
                        />
                    ))}
                    </tbody>
                </table>
            </div>

            <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
            />
        </div>
    );
}

// Export pagination utilities
export const calculatePagination = (
    totalItems: number,
    itemsPerPage: number = ORDER.PAGINATION.ORDERS_PER_PAGE
) => {
    return {
        totalPages: Math.ceil(totalItems / itemsPerPage),
        itemsPerPage
    };
};