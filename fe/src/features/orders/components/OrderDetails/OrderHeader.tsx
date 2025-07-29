// fe/src/features/orders/components/OrderDetails/OrderHeader.tsx
'use client';

import { ArrowLeft } from 'lucide-react';
import { OrderDetail } from '@/features/orders/types/orders.detail.types';
import { CancelOrderButtonLarge } from '@/features/orders/components/CancelOrderButton';

interface OrderHeaderProps {
    order: OrderDetail;
    onBack: () => void;
}

export function OrderHeader({ order, onBack }: OrderHeaderProps) {
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
                <CancelOrderButtonLarge
                    orderId={order.orderId.toString()}
                    orderStatus={order.status}
                />
            </div>

            {/* Order meta information */}
            <div className="mt-4 pt-4 border-t border-gray-100">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                    <div>
                        <span className="text-[#718096]">Order Date:</span>
                        <span className="ml-2 font-medium text-[#1a1a1a]">
                            {new Date(order.createdAt).toLocaleDateString()}
                        </span>
                    </div>
                    <div>
                        <span className="text-[#718096]">Status:</span>
                        <span className="ml-2 font-medium text-[#1a1a1a]">
                            {order.status.replace('_', ' ')}
                        </span>
                    </div>
                    <div>
                        <span className="text-[#718096]">Total Amount:</span>
                        <span className="ml-2 font-medium text-[#f6d55c]">
                            ${order.totalAmount?.toFixed(2) || '0.00'}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
}