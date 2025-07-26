// fe/src/features/orders/components/OrderDetails/OrderDetailsCard.tsx
'use client';

import { Package, User, Mail, MapPin } from 'lucide-react';
import { OrderDetail } from '@/features/orders/types/orders.detail.types';

interface OrderDetailsCardProps {
    order: OrderDetail;
}

export function OrderDetailsCard({ order }: OrderDetailsCardProps) {
    // Parse order description to extract items
    const parseOrderItems = (description: string) => {
        // Example: "Laptop (x1) - $999.99, Mouse (x2) - $50.00"
        const items = description.split(', ').map(item => {
            const match = item.match(/^(.+?)\s*\(x(\d+)\)\s*-\s*\$(.+)$/);
            if (match) {
                const [, name, quantity, totalPrice] = match;
                const qty = parseInt(quantity);
                const total = parseFloat(totalPrice);
                const unitPrice = total / qty;
                return {
                    name: name.trim(),
                    quantity: qty,
                    unitPrice,
                    totalPrice: total
                };
            }
            return {
                name: item,
                quantity: 1,
                unitPrice: 0,
                totalPrice: 0
            };
        });
        return items;
    };

    const orderItems = parseOrderItems(order.orderDescription);
    const subtotal = orderItems.reduce((sum, item) => sum + item.totalPrice, 0);
    const tax = subtotal * 0.1; // 10% tax
    const total = order.totalAmount;

    return (
        <div className="bg-white rounded-lg shadow-sm">
            {/* Header */}
            <div className="border-b border-gray-100 p-6">
                <div className="flex items-center space-x-3">
                    <div className="p-2 bg-[#f6d55c]/10 rounded-lg">
                        <Package className="w-5 h-5 text-[#f6d55c]" />
                    </div>
                    <h2 className="text-lg font-semibold text-[#1a1a1a]">Order Details</h2>
                </div>
            </div>

            <div className="p-6">
                {/* Items List */}
                <div className="mb-6">
                    <h3 className="text-sm font-medium text-[#718096] uppercase tracking-wide mb-4">
                        Order Items
                    </h3>

                    <div className="space-y-4">
                        {orderItems.map((item, index) => (
                            <div key={index} className="flex items-center justify-between py-3 border-b border-gray-50 last:border-b-0">
                                <div className="flex-1">
                                    <h4 className="font-medium text-[#1a1a1a]">{item.name}</h4>
                                    <p className="text-sm text-[#718096]">
                                        ${item.unitPrice.toFixed(2)} × {item.quantity}
                                    </p>
                                </div>
                                <div className="text-right">
                                    <span className="font-medium text-[#1a1a1a]">
                                        ${item.totalPrice.toFixed(2)}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Pricing Breakdown */}
                <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                    <h3 className="text-sm font-medium text-[#718096] uppercase tracking-wide mb-3">
                        Price Breakdown
                    </h3>

                    <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                            <span className="text-[#718096]">Subtotal:</span>
                            <span className="text-[#1a1a1a]">${subtotal.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between text-sm">
                            <span className="text-[#718096]">Tax (10%):</span>
                            <span className="text-[#1a1a1a]">${tax.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between text-sm">
                            <span className="text-[#718096]">Shipping:</span>
                            <span className="text-[#1a1a1a]">Free</span>
                        </div>
                        <hr className="my-2" />
                        <div className="flex justify-between font-semibold text-lg">
                            <span className="text-[#1a1a1a]">Total:</span>
                            <span className="text-[#f6d55c]">${total.toFixed(2)}</span>
                        </div>
                    </div>
                </div>

                {/* Customer Information */}
                <div>
                    <h3 className="text-sm font-medium text-[#718096] uppercase tracking-wide mb-4">
                        Customer Information
                    </h3>

                    <div className="space-y-3">
                        <div className="flex items-center space-x-3">
                            <User className="w-4 h-4 text-[#718096]" />
                            <div>
                                <span className="text-sm text-[#718096]">Name:</span>
                                <span className="ml-2 font-medium text-[#1a1a1a]">{order.userName}</span>
                            </div>
                        </div>

                        <div className="flex items-center space-x-3">
                            <Mail className="w-4 h-4 text-[#718096]" />
                            <div>
                                <span className="text-sm text-[#718096]">Email:</span>
                                <span className="ml-2 font-medium text-[#1a1a1a]">{order.userEmail}</span>
                            </div>
                        </div>

                        <div className="flex items-center space-x-3">
                            <MapPin className="w-4 h-4 text-[#718096]" />
                            <div>
                                <span className="text-sm text-[#718096]">Customer ID:</span>
                                <span className="ml-2 font-medium text-[#1a1a1a]">{order.userId}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}