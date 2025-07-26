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
    // Exclude ORD_ from orderId
    orderId = orderId.replace('ORD_', '');

    // 🎯 NOW GET LIVE STATUS DATA FROM THE HOOK
    const {
        data,
        isLoading,
        isError,
        error,
        liveOrderStatus,      // Live order status 🔴
        livePaymentStatus,    // Live payment status 🔴
        statusLoading,
        isPaymentLoading,     // ✅ NEW: Payment-specific loading
        hasPaymentData        // ✅ NEW: Whether payment data exists
    } = useOrderDetails(orderId);

    // Loading state
    if (isLoading) {
        return <LoadingSkeleton />;
    }

    // ✅ FIX: Only show error if ORDER fails, not payment
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

    // 🎯 USE LIVE STATUS WITH FALLBACKS
    const currentOrderStatus = liveOrderStatus || order.status;
    const currentPaymentStatus = livePaymentStatus || payment?.status;

    return (
        <div className="min-h-screen bg-[#f7fafc] page-entrance">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Order Header */}
                <div className="header-stagger-1">
                    <OrderHeader
                        order={order}
                        onBack={() => router.push('/orders')}
                    />
                </div>

                {/* Status Timeline - NOW WITH LIVE DATA! */}
                <div className="mb-8 header-stagger-2">
                    {statusLoading && (
                        <div className="text-sm text-[#718096] mb-2 flex items-center gap-2">
                            <div className="w-3 h-3 border-2 border-[#f6d55c] border-t-transparent rounded-full animate-spin"></div>
                            Checking latest status...
                        </div>
                    )}
                    <StatusTimeline
                        orderStatus={currentOrderStatus}     // ✅ Live order status
                        paymentStatus={currentPaymentStatus} // ✅ Live payment status (may be undefined)
                        createdAt={order.createdAt}
                        processedAt={payment?.processedAt}    // ✅ Safe access with optional chaining
                    />
                </div>

                {/* Two-column layout for desktop */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* Order Details Card - slides in from left */}
                    <div className="card-materialize-left">
                        <OrderDetailsCard order={order} />
                    </div>

                    {/* ✅ FIX: Payment Information Card - Handle null payment gracefully */}
                    <div className="card-materialize-right">
                        <PaymentInformationCard
                            payment={payment ? {
                                ...payment,
                                status: currentPaymentStatus || payment.status // Update with live status if available
                            } : null}
                            orderStatus={currentOrderStatus}    // ✅ Pass order status for context
                            isPaymentLoading={isPaymentLoading} // ✅ Pass payment loading state
                        />
                    </div>
                </div>

                {/* Optional: Status Change Indicators */}
                {(liveOrderStatus && liveOrderStatus !== order.status) && (
                    <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg">
                        <p className="text-sm text-green-700">
                            ✅ Order status updated to: <strong>{liveOrderStatus}</strong>
                        </p>
                    </div>
                )}

                {(livePaymentStatus && payment && livePaymentStatus !== payment.status) && (
                    <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                        <p className="text-sm text-blue-700">
                            💳 Payment status updated to: <strong>{livePaymentStatus}</strong>
                        </p>
                    </div>
                )}

                {/* ✅ NEW: Payment Processing Notification for CREATED orders */}
                {currentOrderStatus === 'CREATED' && !hasPaymentData && (
                    <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                        <p className="text-sm text-blue-700">
                            🔄 <strong>Payment Processing:</strong> Your order is being processed. Payment information will appear shortly.
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
}