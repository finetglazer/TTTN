// fe/src/features/orders/views/order-details.tsx
'use client';

import { useOrderDetails } from '@/features/orders/hooks/useOrderDetails';
import { useRouter } from 'next/navigation';
import {
    OrderHeader,
    StatusTimeline,
    OrderDetailsCard,
    PaymentInformationCard,
    LoadingSkeleton
} from '@/features/orders/components/OrderDetails';

interface OrderDetailsPageProps {
    orderId: string;
}

export default function OrderDetailsPage({ orderId }: OrderDetailsPageProps) {
    const router = useRouter();
    //exclude ORD_ from orderId
    orderId = orderId.replace('ORD_', '');
    const { data, isLoading, isError, error } = useOrderDetails(orderId);

    // Loading state
    if (isLoading) {
        return <LoadingSkeleton />;
    }

    // Error state
    if (isError || !data) {
        return (
            <div className="min-h-screen bg-[#f7fafc]">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                    <div className="bg-white rounded-lg shadow-sm p-8 text-center">
                        <div className="text-red-500 text-6xl mb-4">⚠️</div>
                        <h2 className="text-2xl font-bold text-[#1a1a1a] mb-2">
                            Order Not Found
                        </h2>
                        <p className="text-[#718096] mb-6">
                            {error?.message || 'The order you are looking for could not be found.'}
                        </p>
                        <button
                            onClick={() => router.push('/orders')}
                            className="bg-[#f6d55c] hover:bg-[#e6c53f] text-[#1a1a1a] px-6 py-3 rounded-lg font-medium transition-colors duration-200"
                        >
                            Back to Orders
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    const { order, payment } = data;

    return (
        <div className="min-h-screen bg-[#f7fafc]">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Order Header */}
                <OrderHeader
                    order={order}
                    onBack={() => router.push('/orders')}
                />

                {/* Status Timeline */}
                <div className="mb-8">
                    <StatusTimeline
                        orderStatus={'CREATED'} // Default for now - will be enhanced with order status polling
                        paymentStatus={payment.status}
                        createdAt={order.createdAt}
                        processedAt={payment.processedAt}
                    />
                </div>

                {/* Two-column layout for desktop */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* Order Details Card */}
                    <OrderDetailsCard order={order} />

                    {/* Payment Information Card */}
                    <PaymentInformationCard payment={payment} />
                </div>
            </div>
        </div>
    );
}