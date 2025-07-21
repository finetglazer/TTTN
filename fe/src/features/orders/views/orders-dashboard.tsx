'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import Header from '@/layouts/components/header';
import { ORDER, FILTER_OPTIONS, COLORS } from '@/core/config/constants';

interface Order {
    id: string;
    orderNumber: string;
    customerName: string;
    amount: number;
    status: keyof typeof ORDER.STATUS;
    createdAt: string;
    items: string[];
}

// Mock data
const mockOrders: Order[] = [
    {
        id: '1',
        orderNumber: 'ORD-001',
        customerName: 'John Doe',
        amount: 125.50,
        status: ORDER.STATUS.DELIVERED,
        createdAt: '2025-01-15',
        items: ['Laptop', 'Mouse']
    },
    {
        id: '2',
        orderNumber: 'ORD-002',
        customerName: 'Jane Smith',
        amount: 89.99,
        status: ORDER.STATUS.CONFIRMED,
        createdAt: '2025-01-14',
        items: ['Headphones']
    },
    {
        id: '3',
        orderNumber: 'ORD-003',
        customerName: 'Bob Johnson',
        amount: 299.00,
        status: ORDER.STATUS.CREATED,
        createdAt: '2025-01-13',
        items: ['Monitor', 'Keyboard', 'Mouse']
    },
    {
        id: '4',
        orderNumber: 'ORD-004',
        customerName: 'Alice Brown',
        amount: 45.75,
        status: ORDER.STATUS.CANCELLED,
        createdAt: '2025-01-12',
        items: ['Cable']
    }
];

export default function DashboardPage() {
    const [orders] = useState<Order[]>(mockOrders);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState<string>('all');
    const [currentPage, setCurrentPage] = useState(1);
    const ordersPerPage = ORDER.PAGINATION.ORDERS_PER_PAGE;

    // Filter and search orders
    const filteredOrders = useMemo(() => {
        return orders.filter(order => {
            const matchesSearch = order.orderNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
                order.customerName.toLowerCase().includes(searchTerm.toLowerCase());
            const matchesStatus = statusFilter === 'all' || order.status === statusFilter;
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

    return (
        <div className="min-h-screen bg-[#f7fafc]">
            {/*<Header />*/}

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
                                placeholder="Search by order number or customer..."
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
                                    key={order.id}
                                    className={`hover:bg-gray-50 transition-colors duration-150 ${
                                        index % 2 === 0 ? 'bg-white' : 'bg-[#fafafa]'
                                    }`}
                                >
                                    <td className="px-6 py-4">
                                        <div>
                                            <div className="font-medium text-[#1a1a1a]">{order.orderNumber}</div>
                                            <div className="text-sm text-[#718096]">
                                                {order.items.join(', ')}
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 text-[#2d3748]">
                                        {order.customerName}
                                    </td>
                                    <td className="px-6 py-4 font-medium text-[#1a1a1a]">
                                        ${order.amount.toFixed(2)}
                                    </td>
                                    <td className="px-6 py-4">
                      <span className={getStatusBadgeClass(order.status)}>
                        {order.status}
                      </span>
                                    </td>
                                    <td className="px-6 py-4 text-[#718096]">
                                        {new Date(order.createdAt).toLocaleDateString()}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2">
                                            <Link
                                                href={`/order/${order.id}`}
                                                className="font-medium"
                                                style={{color: COLORS.PRIMARY_GOLD}}
                                                onMouseEnter={(e) => e.target.style.color = COLORS.GOLD_HOVER}
                                                onMouseLeave={(e) => e.target.style.color = COLORS.PRIMARY_GOLD}
                                            >
                                                View Details
                                            </Link>
                                            {(order.status === ORDER.STATUS.CREATED || order.status === ORDER.STATUS.CONFIRMED) && (
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
                                            : 'text-[#718096] hover:bg-gray-100'
                                    }`}
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
                {filteredOrders.length === 0 && (
                    <div className="text-center py-12">
                        <div className="text-[#718096] text-lg mb-4">No orders found</div>
                        <Link href="/create-order" className="btn-primary">
                            Create Your First Order
                        </Link>
                    </div>
                )}
            </div>
        </div>
    );
}