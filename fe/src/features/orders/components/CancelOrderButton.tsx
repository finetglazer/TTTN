// fe/src/features/orders/components/CancelOrderButton.tsx
'use client';

import { X, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useCancelOrder } from '@/features/orders/hooks/userCancelOrder';
import OrderCancellationNotification from './OrderCancellationNotification';

interface CancelOrderButtonProps {
    orderId: string;
    orderStatus: string;
    variant?: 'default' | 'outline' | 'ghost' | 'destructive';
    size?: 'default' | 'sm' | 'lg' | 'icon';
    className?: string;
    showIcon?: boolean;
    children?: React.ReactNode;
    reason?: string;
}

export function CancelOrderButton({
    orderId,
    orderStatus,
    variant = 'outline',
    size = 'default',
    className = '',
    showIcon = true,
    children,
    reason
}: CancelOrderButtonProps) {
    const {
        cancelOrder,
        closeNotification,
        retryCancel,
        canCancelOrder,
        isLoading,
        notification
    } = useCancelOrder();

    const handleCancel = () => {
        cancelOrder(orderId, reason);
    };

    const handleRetry = () => {
        retryCancel(orderId, reason);
    };

    // Don't render if order cannot be cancelled
    if (!canCancelOrder(orderStatus)) {
        return null;
    }

    return (
        <>
            <Button
                onClick={handleCancel}
                disabled={isLoading}
                variant={variant}
                size={size}
                className={`
                    ${variant === 'outline' ? 'border-red-300 text-red-600 hover:bg-red-50 hover:border-red-400' : ''}
                    ${variant === 'destructive' ? 'bg-red-600 text-white hover:bg-red-700' : ''}
                    transition-all duration-200 focus:ring-4 focus:ring-red-500/30
                    ${className}
                `}
            >
                {isLoading ? (
                    <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        Cancelling...
                    </>
                ) : (
                    <>
                        {showIcon && <X className="w-4 h-4 mr-2" />}
                        {children || 'Cancel Order'}
                    </>
                )}
            </Button>

            {/* Notification Modal */}
            <OrderCancellationNotification
                isOpen={notification.isOpen}
                onClose={closeNotification}
                message={notification.message}
                isSuccess={notification.isSuccess}
                orderId={notification.orderId}
                allowRetry={notification.allowRetry}
                onRetry={handleRetry}
            />
        </>
    );
}

// For table actions - smaller version
export function CancelOrderButtonSmall({
    orderId,
    orderStatus,
    reason
}: {
    orderId: string;
    orderStatus: string;
    reason?: string;
}) {
    return (
        <CancelOrderButton
            orderId={orderId}
            orderStatus={orderStatus}
            variant="outline"
            size="sm"
            className="text-xs"
            showIcon={false}
            reason={reason}
        >
            Cancel
        </CancelOrderButton>
    );
}

// For order header - larger version
export function CancelOrderButtonLarge({
    orderId,
    orderStatus,
    reason
}: {
    orderId: string;
    orderStatus: string;
    reason?: string;
}) {
    return (
        <CancelOrderButton
            orderId={orderId}
            orderStatus={orderStatus}
            variant="outline"
            size="lg"
            className="px-6"
            showIcon={true}
            reason={reason}
        >
            Cancel Order
        </CancelOrderButton>
    );
}