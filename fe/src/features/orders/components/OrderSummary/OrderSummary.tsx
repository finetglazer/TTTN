// fe/src/features/orders/components/OrderSummary/OrderSummary.tsx
'use client';

import { ORDER } from '@/core/config/constants';
import { OrderItem } from '@/features/orders/types/orders.create.types';

interface OrderSummaryProps {
    orderItems: OrderItem[];
    isSubmitting?: boolean;
    onSubmit: () => void;
}

// Sub-component for items summary
function ItemsSummary({ items }: { items: OrderItem[] }) {
    if (items.length === 0) {
        return (
            <p className="text-[#718096] text-center py-8">
                No items in cart
            </p>
        );
    }

    return (
        <div className="space-y-3 mb-6">
            {items.map((item) => (
                <div key={item.id} className="flex justify-between items-center">
                    <div className="flex-1 mr-4">
                        <p className="font-medium text-[#2d3748]">{item.name}</p>
                        <p className="text-sm text-[#718096]">
                            ${item.price.toFixed(2)} × {item.quantity}
                        </p>
                    </div>
                    <span className="font-semibold text-[#1a1a1a]">
                        ${(item.price * item.quantity).toFixed(2)}
                    </span>
                </div>
            ))}
        </div>
    );
}

// Sub-component for price calculations
function PriceCalculation({
                              subtotal,
                              tax,
                              total
                          }: {
    subtotal: number;
    tax: number;
    total: number;
}) {
    return (
        <div className="border-t border-gray-200 pt-4 space-y-2">
            <div className="flex justify-between text-[#2d3748]">
                <span>Subtotal:</span>
                <span>${subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-[#2d3748]">
                <span>Tax ({(ORDER.TAX_RATE * 100).toFixed(0)}%):</span>
                <span>${tax.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-xl font-bold text-[#f6d55c] pt-2 border-t border-gray-200">
                <span>Total:</span>
                <span>${total.toFixed(2)}</span>
            </div>
        </div>
    );
}

// Sub-component for submit section
function SubmitSection({
                           hasItems,
                           isSubmitting,
                           onSubmit
                       }: {
    hasItems: boolean;
    isSubmitting: boolean;
    onSubmit: () => void;
}) {
    return (
        <div className="mt-8">
            <button
                type="submit"
                onClick={onSubmit}
                disabled={!hasItems || isSubmitting}
                className="btn-primary w-full text-lg py-4"
            >
                {isSubmitting ? (
                    <div className="flex items-center justify-center">
                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                        Creating Order...
                    </div>
                ) : (
                    `Create Order`
                )}
            </button>

            {!hasItems && (
                <p className="text-sm text-[#718096] text-center mt-2">
                    Add items to create an order
                </p>
            )}
        </div>
    );
}

// Main OrderSummary component
export default function OrderSummary({
                                         orderItems,
                                         isSubmitting = false,
                                         onSubmit
                                     }: OrderSummaryProps) {
    // Calculate totals
    const subtotal = orderItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const tax = subtotal * ORDER.TAX_RATE;
    const total = subtotal + tax;

    return (
        <div className="lg:col-span-1">
            <div className="card sticky top-8">
                <h2 className="text-xl font-semibold text-[#1a1a1a] mb-6 pb-2 border-b-2 border-[#f6d55c]">
                    Order Summary
                </h2>

                <ItemsSummary items={orderItems} />

                {orderItems.length > 0 && (
                    <PriceCalculation
                        subtotal={subtotal}
                        tax={tax}
                        total={total}
                    />
                )}

                <SubmitSection
                    hasItems={orderItems.length > 0}
                    isSubmitting={isSubmitting}
                    onSubmit={onSubmit}
                />
            </div>
        </div>
    );
}

// Export the calculation utilities for use in parent component
export const calculateOrderTotals = (orderItems: OrderItem[]) => {
    const subtotal = orderItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const tax = subtotal * ORDER.TAX_RATE;
    const total = subtotal + tax;
    return { subtotal, tax, total };
};