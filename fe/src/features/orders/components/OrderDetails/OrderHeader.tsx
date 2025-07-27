// fe/src/features/orders/components/OrderDetails/OrderHeader.tsx
'use client';

import { ArrowLeft, X } from 'lucide-react';
import { OrderDetail } from '@/features/orders/types/orders.detail.types';

interface OrderHeaderProps {
    order: OrderDetail;
    onBack: () => void;
    onCancel?: () => void;
}

export function OrderHeader({ order, onBack, onCancel }: OrderHeaderProps) {
    // const canCancel = order.status === ORDER.STATUS.CREATED; // Now, just assume
    const canCancel = true;

    return (
        <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
            <div className="flex items-center justify-between">
                {/* Left side - Back button and Order info */}
                <div className="flex items-center space-x-4">
                    <button
                        onClick={onBack}
                        className="flex items-center space-x-2 text-[#718096] hover:text-[#1a1a1a] transition-colors duration-200 p-2 hover:bg-gray-50 rounded-lg"
                    >
                        <ArrowLeft className="w-5 h-5" />
                        <span className="font-medium">Back</span>
                    </button>

                    <div className="border-l border-gray-300 pl-4">
                        <h1 className="text-2xl font-bold text-[#f6d55c] mb-1">
                            Order #{order.orderId}
                        </h1>
                        <p className="text-[#718096] text-sm">
                            Customer: <span className="font-medium text-[#1a1a1a]">{order.userName}</span>
                        </p>
                    </div>
                </div>

                {/* Right side - Cancel button (conditional) */}
                {canCancel && (
                    <button
                        onClick={onCancel}
                        className="flex items-center space-x-2 px-4 py-2 border border-red-300 text-red-600 hover:bg-red-50 hover:border-red-400 rounded-lg transition-colors duration-200"
                    >
                        <X className="w-4 h-4" />
                        <span className="font-medium">Cancel Order</span>
                    </button>
                )}
            </div>

            {/* Order meta information */}
            <div className="mt-4 pt-4 border-t border-gray-100">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                    <div>
                        <span className="text-[#718096]">Order Date:</span>
                        <span className="ml-2 font-medium text-[#1a1a1a]">
                            {new Date(order.createdAt).toLocaleDateString('en-US', {
                                year: 'numeric',
                                month: 'long',
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                            })}
                        </span>
                    </div>
                    <div>
                        <span className="text-[#718096]">Customer Email:</span>
                        <span className="ml-2 font-medium text-[#1a1a1a]">
                            {order.userEmail}
                        </span>
                    </div>
                    <div>
                        <span className="text-[#718096]">Total Amount:</span>
                        <span className="ml-2 font-bold text-[#f6d55c] text-lg">
                            ${order.totalAmount.toFixed(2)}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
}