// fe/src/features/orders/components/OrderSuccessNotification.tsx
'use client';

import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { useRouter } from 'next/navigation';
import { CheckCircle, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { COLORS, ROUTES } from '@/core/config/constants';

interface OrderSuccessNotificationProps {
    isOpen: boolean;
    onClose: () => void;
}

export default function OrderSuccessNotification({ isOpen, onClose }: OrderSuccessNotificationProps) {
    const router = useRouter();

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

    const handleGoToDashboard = () => {
        onClose();
        setTimeout(() => {
            router.push(ROUTES.DASHBOARD);
        }, 100);
    };

    const handleStayOnPage = () => {
        onClose();
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

                        {/* Success icon */}
                        <div className="flex justify-center mb-6">
                            <div
                                className="relative p-3 rounded-full animate-pulse-glow"
                                style={{ backgroundColor: '#48bb78' }}
                            >
                                <CheckCircle className="w-8 h-8 text-white" />

                                {/* Animated ring around icon */}
                                <div
                                    className="absolute inset-0 rounded-full animate-ping"
                                    style={{
                                        background: 'rgba(72, 187, 120, 0.3)',
                                        animation: 'ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite'
                                    }}
                                />
                            </div>
                        </div>

                        {/* Content */}
                        <div className="text-center mb-8">
                            <h2
                                id="notification-title"
                                className="text-2xl font-bold mb-3"
                                style={{ color: COLORS.DEEP_CHARCOAL }}
                            >
                                Order Created Successfully!
                            </h2>

                            <p
                                id="notification-description"
                                className="text-base leading-relaxed"
                                style={{ color: '#718096' }}
                            >
                                Your order has been created and is now being processed.
                                <br />
                                Move to the dashboard to see the status of your order.
                            </p>
                        </div>

                        {/* Action buttons */}
                        <div className="space-y-3">
                            {/* Primary button - Go to Dashboard */}
                            <Button
                                onClick={handleGoToDashboard}
                                className="w-full h-12 text-base font-medium btn-primary transition-all duration-200 hover:transform hover:scale-[1.02] focus:ring-4 focus:ring-[#f6d55c]/30"
                                style={{
                                    background: `linear-gradient(45deg, ${COLORS.PRIMARY_GOLD} 0%, #e6c53f 100%)`,
                                    color: COLORS.DEEP_CHARCOAL,
                                    boxShadow: '0 8px 25px rgba(246, 213, 92, 0.3)'
                                }}
                            >
                                Go to Dashboard
                            </Button>

                            {/* Secondary button - Stay on Page */}
                            <Button
                                onClick={handleStayOnPage}
                                variant="outline"
                                className="w-full h-12 text-base font-medium transition-all duration-200 hover:transform hover:scale-[1.02] focus:ring-4 focus:ring-[#f6d55c]/30"
                                style={{
                                    borderColor: COLORS.PRIMARY_GOLD,
                                    color: COLORS.PRIMARY_GOLD,
                                }}
                            >
                                Stay on This Page
                            </Button>
                        </div>

                        {/* Decorative accent line */}
                        <div
                            className="absolute bottom-0 left-0 right-0 h-1"
                            style={{
                                background: `linear-gradient(90deg, transparent 0%, ${COLORS.PRIMARY_GOLD} 50%, transparent 100%)`
                            }}
                        />
                    </div>
                </div>
            </div>
        </>,
        document.body // ← This renders the modal at the root level
    );
}