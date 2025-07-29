// fe/src/features/orders/components/OrderCancellationNotification.tsx
'use client';

import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { AlertCircle, CheckCircle, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { COLORS } from '@/core/config/constants';

interface OrderCancellationNotificationProps {
    isOpen: boolean;
    onClose: () => void;
    message: string;
    isSuccess: boolean;
    orderId?: string;
    allowRetry?: boolean;
    onRetry?: () => void;
}

export default function OrderCancellationNotification({
                                                          isOpen,
                                                          onClose,
                                                          message,
                                                          isSuccess,
                                                          orderId,
                                                          allowRetry = false,
                                                          onRetry
                                                      }: OrderCancellationNotificationProps) {

    // Close on escape key
    useEffect(() => {
        const handleEscape = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
            // Prevent body scroll when modal is open
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
            document.body.style.overflow = 'unset';
        };
    }, [isOpen, onClose]);

    const handleRetry = () => {
        onClose();
        if (onRetry) {
            setTimeout(() => onRetry(), 100);
        }
    };

    if (!isOpen) return null;

    // Create portal to render modal at document root level
    return createPortal(
        <>
            {/* Full-page backdrop overlay with blur */}
            <div
                className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[9998] transition-opacity duration-300"
                onClick={onClose}
                aria-hidden="true"
            />

            {/* Notification modal */}
            <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4">
                <div
                    className="relative w-full max-w-md transform transition-all duration-300 animate-fade-in"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="notification-title"
                    aria-describedby="notification-description"
                >
                    {/* Modal container */}
                    <div
                        className="card relative overflow-hidden"
                        style={{
                            background: 'linear-gradient(135deg, #ffffff 0%, #f7fafc 100%)',
                            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(246, 213, 92, 0.1)'
                        }}
                    >
                        {/* Close button */}
                        <button
                            onClick={onClose}
                            className="absolute top-4 right-4 p-2 rounded-full transition-colors duration-200 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#f6d55c] focus:ring-offset-2"
                            aria-label="Close notification"
                        >
                            <X className="w-5 h-5 text-[#718096]" />
                        </button>

                        {/* Icon and content */}
                        <div className="p-6 pb-0">
                            {/* Status icon */}
                            <div className="flex justify-center mb-4">
                                {isSuccess ? (
                                    <div className="w-16 h-16 rounded-full bg-green-100 flex items-center justify-center">
                                        <CheckCircle className="w-8 h-8 text-green-600" />
                                    </div>
                                ) : (
                                    <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center">
                                        <AlertCircle className="w-8 h-8 text-red-600" />
                                    </div>
                                )}
                            </div>

                            {/* Title */}
                            <h2
                                id="notification-title"
                                className="text-xl font-semibold text-center mb-3"
                                style={{ color: COLORS.DEEP_CHARCOAL }}
                            >
                                {isSuccess ? 'Cancellation Initiated' : 'Cancellation Failed'}
                            </h2>

                            {/* Message */}
                            <div className="text-center mb-6">
                                <p
                                    id="notification-description"
                                    className="text-base leading-relaxed mb-2"
                                    style={{ color: '#718096' }}
                                >
                                    {message}
                                </p>
                                {orderId && (
                                    <p className="text-sm font-medium" style={{ color: COLORS.DEEP_CHARCOAL }}>
                                        Order #{orderId}
                                    </p>
                                )}
                            </div>
                        </div>

                        {/* Action buttons */}
                        <div className="p-6 pt-0">
                            {isSuccess ? (
                                // Success: Just close button
                                <Button
                                    onClick={onClose}
                                    className="w-full h-12 text-base font-medium transition-all duration-200 hover:transform hover:scale-[1.02] focus:ring-4 focus:ring-[#f6d55c]/30"
                                    style={{
                                        background: `linear-gradient(45deg, ${COLORS.PRIMARY_GOLD} 0%, #e6c53f 100%)`,
                                        color: COLORS.DEEP_CHARCOAL,
                                        boxShadow: '0 8px 25px rgba(246, 213, 92, 0.3)'
                                    }}
                                >
                                    Got It
                                </Button>
                            ) : (
                                // Failure: Retry and Close buttons
                                <div className="space-y-3">
                                    {allowRetry && onRetry && (
                                        <Button
                                            onClick={handleRetry}
                                            className="w-full h-12 text-base font-medium transition-all duration-200 hover:transform hover:scale-[1.02] focus:ring-4 focus:ring-red-500/30"
                                            style={{
                                                background: 'linear-gradient(45deg, #f56565 0%, #e53e3e 100%)',
                                                color: 'white',
                                                boxShadow: '0 8px 25px rgba(245, 101, 101, 0.3)'
                                            }}
                                        >
                                            Try Again
                                        </Button>
                                    )}

                                    <Button
                                        onClick={onClose}
                                        variant="outline"
                                        className="w-full h-12 text-base font-medium transition-all duration-200 hover:transform hover:scale-[1.02] focus:ring-4 focus:ring-[#f6d55c]/30"
                                        style={{
                                            borderColor: COLORS.PRIMARY_GOLD,
                                            color: COLORS.PRIMARY_GOLD,
                                        }}
                                    >
                                        Close
                                    </Button>
                                </div>
                            )}
                        </div>

                        {/* Decorative accent line */}
                        <div
                            className="absolute bottom-0 left-0 right-0 h-1"
                            style={{
                                background: isSuccess
                                    ? `linear-gradient(90deg, transparent 0%, #48bb78 50%, transparent 100%)`
                                    : `linear-gradient(90deg, transparent 0%, #f56565 50%, transparent 100%)`
                            }}
                        />
                    </div>
                </div>
            </div>
        </>,
        document.body
    );
}