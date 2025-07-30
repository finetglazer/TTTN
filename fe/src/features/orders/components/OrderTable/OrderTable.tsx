'use client';

import { useState, useMemo } from 'react';
import { ORDER, COLORS } from '@/core/config/constants';
import {OrdersDashboardDisplay, OrdersTableProps} from '@/features/orders/types/orders.dashboard.types';
import {useRouter} from "next/navigation";

// Define types for sorting functionality
type SortableKeys = 'orderId' | 'userName' | 'totalAmount' | 'createdAt';
type SortConfig = {
    key: SortableKeys | null;
    direction: 'ascending' | 'descending';
};

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
            {/*{order.orderStatus === ORDER.STATUS.CREATED && (*/}
            {/* <button*/}
            {/* onClick={handleCancel}*/}
            {/* className="text-red-600 hover:text-red-800 text-sm"*/}
            {/* >*/}
            {/* Cancel*/}
            {/* </button>*/}
            {/*)}*/}
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

// Sub-component for table header with sorting
function TableHeader({
                         onSort,
                         sortConfig,
                     }: {
    onSort: (key: SortableKeys) => void;
    sortConfig: SortConfig;
}) {
    const renderSortArrow = (columnKey: SortableKeys) => {
        if (sortConfig.key !== columnKey) {
            return null; // No icon if not the active sort column
        }
        if (sortConfig.direction === 'ascending') {
            return <span className="ml-1">▲</span>;
        }
        return <span className="ml-1">▼</span>;
    };

    const headerBaseClasses = "px-6 py-4 text-left text-sm font-medium uppercase tracking-wider";
    const sortableHeaderClasses = `${headerBaseClasses} cursor-pointer hover:bg-[#3a4a5b] transition-colors`;

    return (
        <thead className="bg-[#2d3748] text-white">
        <tr>
            <th className={sortableHeaderClasses} onClick={() => onSort('orderId')}>
                Order {renderSortArrow('orderId')}
            </th>
            <th className={sortableHeaderClasses} onClick={() => onSort('userName')}>
                Customer {renderSortArrow('userName')}
            </th>
            <th className={sortableHeaderClasses} onClick={() => onSort('totalAmount')}>
                Amount {renderSortArrow('totalAmount')}
            </th>
            <th className={headerBaseClasses}>
                Status
            </th>
            <th className={sortableHeaderClasses} onClick={() => onSort('createdAt')}>
                Date {renderSortArrow('createdAt')}
            </th>
            <th className={headerBaseClasses}>
                Actions
            </th>
        </tr>
        </thead>
    );
}

// Sub-component for pagination
// Enhanced Pagination Component with Smart Page Range
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

    // Configuration for pagination display
    const PAGES_TO_SHOW = 5; // Number of page buttons to show around current page
    const ALWAYS_SHOW_FIRST_LAST = true; // Always show first and last page

    // Helper function to generate page numbers to display
    const getPageNumbers = () => {
        const pages = [];

        // If total pages is small, show all pages
        if (totalPages <= PAGES_TO_SHOW + 2) {
            return Array.from({ length: totalPages }, (_, i) => i + 1);
        }

        // Always include first page
        if (ALWAYS_SHOW_FIRST_LAST) {
            pages.push(1);
        }

        // Calculate the range around current page
        const startPage = Math.max(2, currentPage - Math.floor(PAGES_TO_SHOW / 2));
        const endPage = Math.min(totalPages - 1, startPage + PAGES_TO_SHOW - 1);

        // Add ellipsis after first page if needed
        if (startPage > 2) {
            pages.push('ellipsis-start');
        }

        // Add pages in the middle range
        for (let i = startPage; i <= endPage; i++) {
            if (i !== 1 && i !== totalPages) { // Don't duplicate first/last pages
                pages.push(i);
            }
        }

        // Add ellipsis before last page if needed
        if (endPage < totalPages - 1) {
            pages.push('ellipsis-end');
        }

        // Always include last page
        if (ALWAYS_SHOW_FIRST_LAST && totalPages > 1) {
            pages.push(totalPages);
        }

        return pages;
    };

    const pageNumbers = getPageNumbers();

    // Button styling helper
    const getButtonStyle = (page: number) => ({
        backgroundColor: currentPage === page ? COLORS.PRIMARY_GOLD : undefined,
        color: currentPage === page ? COLORS.DEEP_CHARCOAL : undefined
    });

    const getButtonClassName = (page: number) =>
        `px-3 py-2 rounded-lg text-sm font-medium transition-colors min-w-[40px] ${
            currentPage === page
                ? 'text-black'
                : 'text-[#718096] hover:bg-gray-100'
        }`;

    return (
        <div className="flex items-center justify-center space-x-2 mt-6 pt-6 border-t">
            {/* Previous Button */}
            <button
                onClick={() => onPageChange(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
                className="pagination-btn btn-ghost disabled:opacity-50 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            >
                Previous
            </button>

            {/* Page Numbers */}
            <div className="flex items-center space-x-1">
                {pageNumbers.map((page, index) => {
                    if (typeof page === 'string' && page.startsWith('ellipsis')) {
                        return (
                            <span
                                key={page}
                                className="px-2 py-2 text-[#718096] text-sm"
                            >
                                ...
                            </span>
                        );
                    }

                    const pageNumber = page as number;
                    return (
                        <button
                            key={pageNumber}
                            onClick={() => onPageChange(pageNumber)}
                            className={getButtonClassName(pageNumber)}
                            style={getButtonStyle(pageNumber)}
                        >
                            {pageNumber}
                        </button>
                    );
                })}
            </div>

            {/* Next Button */}
            <button
                onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
                disabled={currentPage === totalPages}
                className="pagination-btn btn-ghost disabled:opacity-50 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            >
                Next
            </button>
        </div>
    );
}

// Optional: Enhanced version with jump-to-page functionality
function PaginationWithJump({
                                currentPage,
                                totalPages,
                                onPageChange
                            }: {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}) {
    const [jumpPage, setJumpPage] = useState('');

    const handleJumpSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        const page = parseInt(jumpPage);
        if (page >= 1 && page <= totalPages) {
            onPageChange(page);
            setJumpPage('');
        }
    };

    return (
        <div className="flex flex-col items-center space-y-4 mt-6 pt-6 border-t">
            {/* Main Pagination */}
            <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={onPageChange}
            />

            {/* Jump to Page (for very large datasets) */}
            {totalPages > 20 && (
                <form onSubmit={handleJumpSubmit} className="flex items-center space-x-2 text-sm">
                    <span className="text-[#718096]">Go to page:</span>
                    <input
                        type="number"
                        min="1"
                        max={totalPages}
                        value={jumpPage}
                        onChange={(e) => setJumpPage(e.target.value)}
                        className="w-16 px-2 py-1 border border-gray-300 rounded text-center"
                        placeholder="1"
                    />
                    <span className="text-[#718096]">of {totalPages}</span>
                    <button
                        type="submit"
                        className="px-3 py-1 bg-gray-100 hover:bg-gray-200 rounded text-[#718096] hover:text-black transition-colors"
                    >
                        Go
                    </button>
                </form>
            )}
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

// Main OrdersTable component with sorting
export default function OrdersTable({
                                        orders,
                                        isLoading = false,
                                        error = null,
                                    }: OrdersTableProps) {
    const [currentPage, setCurrentPage] = useState(1);
    const [sortConfig, setSortConfig] = useState<SortConfig>({ key: null, direction: 'ascending' });
    const ordersPerPage = ORDER.PAGINATION.ORDERS_PER_PAGE;

    const handleRequestSort = (key: SortableKeys) => {
        let direction: 'ascending' | 'descending' = 'ascending';
        if (sortConfig.key === key && sortConfig.direction === 'ascending') {
            direction = 'descending';
        }
        setSortConfig({ key, direction });
    };

    const sortedOrders = useMemo(() => {
        const sortableItems = [...orders];
        if (sortConfig.key) {
            sortableItems.sort((a, b) => {
                const aValue = a[sortConfig.key!];
                const bValue = b[sortConfig.key!];

                if (aValue < bValue) {
                    return sortConfig.direction === 'ascending' ? -1 : 1;
                }
                if (aValue > bValue) {
                    return sortConfig.direction === 'ascending' ? 1 : -1;
                }
                return 0;
            });
        }
        return sortableItems;
    }, [orders, sortConfig]);

    // Pagination calculations
    const totalPages = Math.ceil(sortedOrders.length / ordersPerPage);
    const startIndex = (currentPage - 1) * ordersPerPage;
    const paginatedOrders = sortedOrders.slice(startIndex, startIndex + ordersPerPage);

    // Reset pagination when orders or sorting changes
    useMemo(() => {
        setCurrentPage(1);
    }, [orders.length, sortConfig]);

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
                    <TableHeader onSort={handleRequestSort} sortConfig={sortConfig} />
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