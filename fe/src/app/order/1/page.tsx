'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import Header from '@/components/Layout/Header';

interface OrderDetail {
    id: string;
    orderNumber: string;
    customerName: string;
    customerEmail: string;
    customerPhone: string;
    shippingAddress: string;
    status: 'CREATED' | 'CONFIRMED' | 'DELIVERED' | 'CANCELLED';
    createdAt: string;
    confirmedAt?: string;
    deliveredAt?: string;
    items: Array<{
        id: string;
        name: string;
        price: number;
        quantity: number;
    }>;
    subtotal: number;
    tax: number;
    total: number;
    paymentStatus: 'PENDING' | 'CONFIRMED' | 'FAILED';
    paymentMethod: string;
    transactionId?: string;
}

// Mock data
const mockOrderDetail: OrderDetail = {
    id: '1',
    orderNumber: 'ORD-001',
    customerName: 'John Doe',
    customerEmail: 'john.doe@example.com',
    customerPhone: '+1 (555) 123-4567',
    shippingAddress: '123 Main St, Springfield, IL 62701',
    status: 'DELIVERED',
    createdAt: '2025-01-15T10:00:00Z',
    confirmedAt: '2025-01-15T11:30:00Z',
    deliveredAt: '2025-01-16T14:20:00Z',
    items: [
        {
            id: '1',
            name: 'MacBook Pro 14"',
            price: 1999.00,
            quantity: 1
        },
        {
            id: '2',
            name: 'Magic Mouse',
            price: 79.00,
            quantity: 1
        }
    ],
    subtotal: 2078.00,
    tax: 207.80,
    total: 2285.80,
    paymentStatus: 'CONFIRMED',
    paymentMethod: 'Credit Card ****1234',
    transactionId: 'TXN-ABC123'
};

export default function OrderDetailPage() {
    const params = useParams();
    const [order, setOrder] = useState<OrderDetail | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Mock API call
        setTimeout(() => {
            setOrder(mockOrderDetail);
            setLoading(false);
        }, 500);
    }, [params.id]);

    if (loading) {
        return (
            <div className="min-h-screen bg-[#f7fafc]">
                <Header />
                <div className="max-w-7xl mx-auto px-4 py-8">
                    <div className="animate-pulse">
                        <div className="h-8 bg-gray-300 rounded w-1/4 mb-4"></div>
                        <div className="h-64 bg-gray-300 rounded"></div>
                    </div>
                </div>
            </div>
        );
    }

    if (!order) {
        return (
            <div className="min-h-screen bg-[#f7fafc]">
                <Header />
                <div className="max-w-7xl mx-auto px-4 py-8 text-center">
                    <h1 className="text-2xl font-bold text-[#1a1a1a] mb-4">Order Not Found</h1>
                    <Link href="/dashboard" className="btn-primary">
                        Back to Dashboard
                    </Link>
                </div>
            </div>
        );
    }

    const getStatusSteps = () => {
        const steps = [
            { key: 'CREATED', label: 'Created', timestamp: order.createdAt },
            { key: 'CONFIRMED', label: 'Confirmed', timestamp: order.confirmedAt },
            { key: 'DELIVERED', label: 'Delivered', timestamp: order.deliveredAt }
        ];

        const currentStepIndex = steps.findIndex(step => step.key === order.status);

        return steps.map((step, index) => ({
            ...step,
            isCompleted: index <= currentStepIndex,
            isCurrent: index === currentStepIndex,
            isFuture: index > currentStepIndex
        }));
    };

    const statusSteps = getStatusSteps();

    const getPaymentStatusClass = (status: string) => {
        switch (status) {
            case 'CONFIRMED': return 'status-delivered';
            case 'PENDING': return 'status-confirmed';
            case 'FAILED': return 'status-cancelled';
            default: return 'bg-gray-500 text-white';
        }
    };

    return (
        <div className="min-h-screen bg-[#f7fafc]">
            <Header />

            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Order Header */}
                <div className="card mb-8">
                    <div className="flex items-center justify-between mb-6">
                        <div className="flex items-center gap-4">
                            <Link
                                href="/dashboard"
                                className="text-[#718096] hover:text-[#2d3748] transition-colors"
                            >
                                ← Back
                            </Link>
                            <div>
                                <h1 className="text-3xl font-bold text-[#f6d55c]">
                                    {order.orderNumber}
                                </h1>
                                <p className="text-[#718096]">{order.customerName}</p>
                            </div>
                        </div>

                        {(order.status === 'CREATED' || order.status === 'CONFIRMED') && (
                            <button className="text-[#f56565] hover:text-red-700 font-medium border border-[#f56565] px-4 py-2 rounded-lg">
                                Cancel Order
                            </button>
                        )}
                    </div>
                </div>

                {/* Status Timeline */}
                <div className="card mb-8">
                    <h2 className="text-xl font-semibold text-[#1a1a1a] mb-6 pb-2 border-b-2 border-[#f6d55c]">
                        Order Status
                    </h2>

                    <div className="flex items-center justify-between relative">
                        {/* Progress Line */}
                        <div className="absolute top-6 left-0 right-0 h-0.5 bg-gray-200 z-0">
                            <div
                                className="h-full bg-[#f6d55c] transition-all duration-500"
                                style={{ width: `${((statusSteps.filter(s => s.isCompleted).length - 1) / (statusSteps.length - 1)) * 100}%` }}
                            />
                        </div>

                        {statusSteps.map((step, index) => (
                            <div key={step.key} className="flex flex-col items-center relative z-10">
                                <div className={`w-12 h-12 rounded-full flex items-center justify-center border-4 transition-all duration-300 ${
                                    step.isCompleted
                                        ? 'bg-[#f6d55c] border-[#f6d55c] text-[#1a1a1a]'
                                        : step.isCurrent
                                            ? 'bg-white border-[#f6d55c] text-[#f6d55c] animate-pulse-glow'
                                            : 'bg-white border-gray-300 text-gray-400'
                                }`}>
                                    {step.isCompleted ? '✓' : index + 1}
                                </div>
                                <div className="text-center mt-3">
                                    <div className={`font-medium ${step.isCompleted || step.isCurrent ? 'text-[#1a1a1a]' : 'text-gray-400'}`}>
                                        {step.label}
                                    </div>
                                    {step.timestamp && step.isCompleted && (
                                        <div className="text-sm text-[#718096] mt-1">
                                            {new Date(step.timestamp).toLocaleDateString()} at{' '}
                                            {new Date(step.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Order Details Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* Order Details */}
                    <div className="card">
                        <h2 className="text-xl font-semibold text-[#1a1a1a] mb-6 pb-2 border-b-2 border-[#f6d55c]">
                            Order Details
                        </h2>

                        {/* Items List */}
                        <div className="space-y-4 mb-6">
                            {order.items.map((item) => (
                                <div key={item.id} className="flex justify-between items-center py-3 border-b border-gray-100 last:border-b-0">
                                    <div className="flex-1">
                                        <h4 className="font-medium text-[#1a1a1a]">{item.name}</h4>
                                        <p className="text-sm text-[#718096]">
                                            Quantity: {item.quantity} × ${item.price.toFixed(2)}
                                        </p>
                                    </div>
                                    <div className="font-medium text-[#1a1a1a]">
                                        ${(item.price * item.quantity).toFixed(2)}
                                    </div>
                                </div>
                            ))}
                        </div>

                        {/* Pricing */}
                        <div className="space-y-2 pt-4 border-t">
                            <div className="flex justify-between text-[#718096]">
                                <span>Subtotal:</span>
                                <span>${order.subtotal.toFixed(2)}</span>
                            </div>
                            <div className="flex justify-between text-[#718096]">
                                <span>Tax:</span>
                                <span>${order.tax.toFixed(2)}</span>
                            </div>
                            <div className="flex justify-between text-2xl font-bold text-[#f6d55c] pt-2 border-t">
                                <span>Total:</span>
                                <span>${order.total.toFixed(2)}</span>
                            </div>
                        </div>

                        {/* Customer Information */}
                        <div className="mt-8 pt-6 border-t">
                            <h3 className="font-medium text-[#1a1a1a] mb-4">Customer Information</h3>
                            <div className="space-y-2 text-sm">
                                <div><span className="text-[#718096]">Email:</span> {order.customerEmail}</div>
                                <div><span className="text-[#718096]">Phone:</span> {order.customerPhone}</div>
                                <div><span className="text-[#718096]">Address:</span> {order.shippingAddress}</div>
                            </div>
                        </div>
                    </div>

                    {/* Payment Information */}
                    <div className="card">
                        <h2 className="text-xl font-semibold text-[#1a1a1a] mb-6 pb-2 border-b-2 border-[#f6d55c]">
                            Payment Information
                        </h2>

                        <div className="space-y-4">
                            <div>
                                <label className="text-sm text-[#718096]">Payment Status</label>
                                <div className="mt-1">
                  <span className={`status-badge ${getPaymentStatusClass(order.paymentStatus)}`}>
                    {order.paymentStatus}
                  </span>
                                </div>
                            </div>

                            <div>
                                <label className="text-sm text-[#718096]">Payment Method</label>
                                <div className="mt-1 text-[#1a1a1a] font-medium">{order.paymentMethod}</div>
                            </div>

                            <div>
                                <label className="text-sm text-[#718096]">Total Amount</label>
                                <div className="mt-1 text-2xl font-bold text-[#1a1a1a]">${order.total.toFixed(2)}</div>
                            </div>

                            {order.transactionId && (
                                <div>
                                    <label className="text-sm text-[#718096]">Transaction ID</label>
                                    <div className="mt-1 text-[#1a1a1a] font-mono text-sm">{order.transactionId}</div>
                                </div>
                            )}

                            {order.paymentStatus === 'FAILED' && (
                                <button className="w-full btn-primary mt-4">
                                    Retry Payment
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}