'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import { ORDER, FILTER_OPTIONS, COLORS } from '@/core/config/constants';
import { OrdersDashboardDisplay } from '@/features/orders/types/orders.dashboard.types';

interface OrderDashboardProps {
    orders: OrdersDashboardDisplay[];
    isLoading?: boolean;
    error?: Error | null;
}

export default function OrderDashboard({ orders, isLoading, error }: OrderDashboardProps) {
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState<string>('all');
    const [currentPage, setCurrentPage] = useState(1);
    const ordersPerPage = ORDER.PAGINATION.ORDERS_PER_PAGE;

    // Filter and search orders
    const filteredOrders = useMemo(() => {
        return orders.filter(order => {
            const matchesSearch = order.orderId.toLowerCase().includes(searchTerm.toLowerCase()) ||
                order.userName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                order.orderDescription.toLowerCase().includes(searchTerm.toLowerCase());
            const matchesStatus = statusFilter === 'all' || order.orderStatus === statusFilter;
            return matchesSearch && matchesStatus;
        });
    }, [orders, searchTerm, statusFilter]);

    // Pagination
    const totalPages = Math.ceil(filteredOrders.length / ordersPerPage);
    const startIndex = (currentPage - 1) * ordersPerPage;
    const paginatedOrders = filteredOrders.slice(startIndex, startIndex + ordersPerPage);

    const getStatusBadgeClass = (status: string) => {
        const baseClass = 'status-badge ';
        switch (status) {
            case ORDER.STATUS.CREATED: return baseClass + 'status-created';
            case ORDER.STATUS.CONFIRMED: return baseClass + 'status-confirmed';
            case ORDER.STATUS.DELIVERED: return baseClass + 'status-delivered';
            case ORDER.STATUS.CANCELLED: return baseClass + 'status-cancelled';
            default: return baseClass + 'bg-gray-500 text-white';
        }
    };

    // Reset pagination when filters change
    useMemo(() => {
        setCurrentPage(1);
    }, [searchTerm, statusFilter]);

    if (isLoading) {
        return (
            <div className="min-h-screen bg-[#f7fafc]">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                    {/* Page Header */}
                    <div className="flex justify-between items-center mb-8">
                        <div>
                            <h1 className="text-3xl font-bold text-[#1a1a1a] mb-2">Orders Dashboard</h1>
                            <p className="text-[#718096]">Manage and track all your orders</p>
                        </div>
                        <Link href="/create-order" className="btn-primary">
                            Create New Order
                        </Link>
                    </div>

                    {/* Loading State */}
                    <div className="card">
                        <div className="flex justify-center items-center py-12">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#d69e2e]"></div>
                            <span className="ml-3 text-[#718096]">Loading orders...</span>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-[#f7fafc]">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                    {/* Page Header */}
                    <div className="flex justify-between items-center mb-8">
                        <div>
                            <h1 className="text-3xl font-bold text-[#1a1a1a] mb-2">Orders Dashboard</h1>
                            <p className="text-[#718096]">Manage and track all your orders</p>
                        </div>
                        <Link href="/create-order" className="btn-primary">
                            Create New Order
                        </Link>
                    </div>

                    {/* Error State */}
                    <div className="card">
                        <div className="text-center py-12">
                            <div className="text-red-600 text-lg mb-4">Error loading orders</div>
                            <p className="text-[#718096] mb-4">{error.message}</p>
                            <button
                                onClick={() => window.location.reload()}
                                className="btn-primary"
                            >
                                Try Again
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#f7fafc]">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Page Header */}
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-[#1a1a1a] mb-2">Orders Dashboard</h1>
                        <p className="text-[#718096]">Manage and track all your orders</p>
                    </div>
                    <Link href="/create-order" className="btn-primary">
                        Create New Order
                    </Link>
                </div>

                {/* Filters Bar */}
                <div className="card mb-8">
                    <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                        {/* Search */}
                        <div className="flex-1 max-w-md">
                            <input
                                type="text"
                                placeholder="Search by order ID, customer, or description..."
                                className="form-input"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>

                        {/* Status Filter */}
                        <div className="flex items-center gap-4">
                            <select
                                className="form-input"
                                value={statusFilter}
                                onChange={(e) => setStatusFilter(e.target.value)}
                            >
                                {FILTER_OPTIONS.map(option => (
                                    <option key={option.value} value={option.value}>{option.label}</option>
                                ))}
                            </select>

                            <button
                                onClick={() => {
                                    setSearchTerm('');
                                    setStatusFilter('all');
                                }}
                                className="btn-ghost"
                            >
                                Clear Filters
                            </button>
                        </div>
                    </div>
                </div>

                {/* Orders Table */}
                <div className="card">
                    <div className="overflow-x-auto">
                        <table className="w-full">
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
                            <tbody className="bg-white divide-y divide-gray-200">
                            {paginatedOrders.map((order, index) => (
                                <tr
                                    key={order.orderId}
                                    className={`hover:bg-gray-50 transition-colors duration-150 ${
                                        index % 2 === 0 ? 'bg-white' : 'bg-[#fafafa]'
                                    }`}
                                >
                                    <td className="px-6 py-4">
                                        <div>
                                            <div className="font-medium text-[#1a1a1a]">{order.orderId}</div>
                                            <div className="text-sm text-[#718096]">
                                                {order.orderDescription}
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 text-[#2d3748]">
                                        {order.userName}
                                    </td>
                                    <td className="px-6 py-4 font-medium text-[#1a1a1a]">
                                        ${order.totalAmount.toFixed(2)}
                                    </td>
                                    <td className="px-6 py-4">
                                      <span className={getStatusBadgeClass(order.orderStatus)}>
                                        {order.orderStatus}
                                      </span>
                                    </td>
                                    <td className="px-6 py-4 text-[#718096]">
                                        {new Date(order.createdAt).toLocaleDateString()}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2">
                                            <Link
                                                href={`/order/${order.orderId}`}
                                                className="font-medium"
                                                style={{color: COLORS.PRIMARY_GOLD}}
                                                onMouseEnter={(e) => e.currentTarget.style.color = COLORS.GOLD_HOVER}
                                                onMouseLeave={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}
                                            >
                                                View Details
                                            </Link>
                                            {(order.orderStatus === ORDER.STATUS.CREATED || order.orderStatus === ORDER.STATUS.CONFIRMED) && (
                                                <button className="text-[#f56565] hover:text-red-700 text-sm ml-2">
                                                    Cancel
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Pagination */}
                    {totalPages > 1 && (
                        <div className="flex items-center justify-center space-x-2 mt-6 pt-6 border-t">
                            <button
                                onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                                disabled={currentPage === 1}
                                className="btn-ghost disabled:opacity-50"
                            >
                                Previous
                            </button>

                            {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                                <button
                                    key={page}
                                    onClick={() => setCurrentPage(page)}
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
                                onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
                                disabled={currentPage === totalPages}
                                className="btn-ghost disabled:opacity-50"
                            >
                                Next
                            </button>
                        </div>
                    )}
                </div>

                {/* Empty State */}
                {filteredOrders.length === 0 && !isLoading && (
                    <div className="text-center py-12">
                        <div className="text-[#718096] text-lg mb-4">
                            {orders.length === 0 ? 'No orders found' : 'No orders match your search criteria'}
                        </div>
                        {orders.length === 0 && (
                            <Link href="/create-order" className="btn-primary">
                                Create Your First Order
                            </Link>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}