// fe/src/features/orders/components/OrderDetails/StatusTimeline.tsx
'use client';

import { Check, Clock, Package, Truck } from 'lucide-react';
import { ORDER, PAYMENT } from '@/core/config/constants';

interface StatusTimelineProps {
    orderStatus: string;
    paymentStatus: string | undefined;
    createdAt: string;
    processedAt?: string;
}

interface Step {
    id: string;
    label: string;
    icon: React.ReactNode;
    description: string;
}

export function StatusTimeline({ orderStatus, paymentStatus, createdAt, processedAt }: StatusTimelineProps) {
    const steps: Step[] = [
        {
            id: 'created',
            label: 'Created',
            icon: <Clock className="w-5 h-5" />,
            description: 'Order has been created'
        },
        {
            id: 'confirmed',
            label: 'Confirmed',
            icon: <Check className="w-5 h-5" />,
            description: 'Payment confirmed'
        },
        {
            id: 'delivered',
            label: 'Delivered',
            icon: <Package className="w-5 h-5" />,
            description: 'Order delivered'
        }
    ];

    const getStepStatus = (stepId: string) => {
        const currentOrderIndex = Object.values(ORDER.STATUS).indexOf(orderStatus as any);
        const stepIndex = steps.findIndex(step => step.id === stepId);

        if (stepId === 'created') {
            return 'completed';
        }

        if (stepId === 'confirmed') {
            if (paymentStatus === PAYMENT.STATUS.CONFIRMED) {
                return 'completed';
            }
            if (paymentStatus === PAYMENT.STATUS.PENDING) {
                return 'current';
            }
            if (paymentStatus === PAYMENT.STATUS.FAILED || paymentStatus === PAYMENT.STATUS.DECLINED) {
                return 'failed';
            }
            return 'pending';
        }

        if (stepId === 'delivered') {
            if (orderStatus === ORDER.STATUS.DELIVERED) {
                return 'completed';
            }
            if (orderStatus === ORDER.STATUS.CONFIRMED && paymentStatus === PAYMENT.STATUS.CONFIRMED) {
                return 'current';
            }
            return 'pending';
        }

        return 'pending';
    };

    const getStepClasses = (stepId: string) => {
        const status = getStepStatus(stepId);

        switch (status) {
            case 'completed':
                return {
                    circle: 'bg-[#f6d55c] border-[#f6d55c] text-[#1a1a1a]',
                    line: 'bg-[#f6d55c]',
                    label: 'text-[#1a1a1a] font-medium',
                    description: 'text-[#718096]'
                };
            case 'current':
                return {
                    circle: 'bg-[#f6d55c] border-[#f6d55c] text-[#1a1a1a] shadow-lg ring-4 ring-[#f6d55c]/20',
                    line: 'bg-gray-300',
                    label: 'text-[#f6d55c] font-semibold',
                    description: 'text-[#1a1a1a]'
                };
            case 'failed':
                return {
                    circle: 'bg-red-100 border-red-300 text-red-600',
                    line: 'bg-gray-300',
                    label: 'text-red-600 font-medium',
                    description: 'text-red-500'
                };
            default:
                return {
                    circle: 'bg-white border-gray-300 text-gray-400',
                    line: 'bg-gray-300',
                    label: 'text-[#718096]',
                    description: 'text-[#718096]'
                };
        }
    };

    const getTimestamp = (stepId: string) => {
        if (stepId === 'created') {
            return new Date(createdAt).toLocaleString();
        }
        if (stepId === 'confirmed' && processedAt && paymentStatus === PAYMENT.STATUS.CONFIRMED) {
            return new Date(processedAt).toLocaleString();
        }
        return null;
    };

    return (
        <div className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-lg font-semibold text-[#1a1a1a] mb-6">Order Progress</h2>

            <div className="relative">
                {/* Timeline line */}
                <div className="absolute top-6 left-6 w-0.5 h-full bg-gray-200 -z-10"></div>

                {steps.map((step, index) => {
                    const classes = getStepClasses(step.id);
                    const timestamp = getTimestamp(step.id);
                    const isLast = index === steps.length - 1;

                    return (
                        <div key={step.id} className="relative flex items-start pb-8 last:pb-0">
                            {/* Circle with icon */}
                            <div className={`flex items-center justify-center w-12 h-12 rounded-full border-2 ${classes.circle} transition-all duration-300`}>
                                {step.icon}
                            </div>

                            {/* Content */}
                            <div className="ml-4 flex-1">
                                <div className="flex items-center justify-between">
                                    <h3 className={`text-base font-medium ${classes.label}`}>
                                        {step.label}
                                    </h3>
                                    {timestamp && (
                                        <span className="text-sm text-[#718096]">
                                            {timestamp}
                                        </span>
                                    )}
                                </div>
                                <p className={`text-sm mt-1 ${classes.description}`}>
                                    {step.description}
                                </p>

                                {/* Payment status indicator for confirmed step */}
                                {step.id === 'confirmed' && (
                                    <div className="mt-2">
                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                            paymentStatus === PAYMENT.STATUS.CONFIRMED
                                                ? 'bg-green-100 text-green-800'
                                                : paymentStatus === PAYMENT.STATUS.PENDING
                                                    ? 'bg-yellow-100 text-yellow-800'
                                                    : paymentStatus === PAYMENT.STATUS.FAILED || paymentStatus === PAYMENT.STATUS.DECLINED
                                                        ? 'bg-red-100 text-red-800'
                                                        : 'bg-gray-100 text-gray-800'
                                        }`}>
                                            Payment: {paymentStatus}
                                        </span>
                                    </div>
                                )}
                            </div>

                            {/* Connecting line to next step */}
                            {!isLast && (
                                <div className={`absolute top-12 left-6 w-0.5 h-8 ${classes.line}`}></div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}