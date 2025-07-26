// fe/src/features/orders/components/OrderDetails/PaymentInformationCard.tsx
'use client';

import { CreditCard, CheckCircle, XCircle, Clock, AlertTriangle, RefreshCw } from 'lucide-react';
import { PAYMENT } from '@/core/config/constants';
import { PaymentDetail } from '@/features/payments/types/payment.detail.types';

interface PaymentInformationCardProps {
    payment: PaymentDetail;
    onRetryPayment?: () => void;
}

export function PaymentInformationCard({ payment, onRetryPayment }: PaymentInformationCardProps) {
    const getPaymentStatusIcon = (status: string) => {
        switch (status) {
            case PAYMENT.STATUS.CONFIRMED:
                return <CheckCircle className="w-5 h-5 text-green-600" />;
            case PAYMENT.STATUS.FAILED:
            case PAYMENT.STATUS.DECLINED:
                return <XCircle className="w-5 h-5 text-red-600" />;
            case PAYMENT.STATUS.PENDING:
                return <Clock className="w-5 h-5 text-yellow-600" />;
            default:
                return <AlertTriangle className="w-5 h-5 text-gray-600" />;
        }
    };

    const getPaymentStatusBadge = (status: string) => {
        switch (status) {
            case PAYMENT.STATUS.CONFIRMED:
                return 'bg-green-100 text-green-800 border-green-200';
            case PAYMENT.STATUS.FAILED:
            case PAYMENT.STATUS.DECLINED:
                return 'bg-red-100 text-red-800 border-red-200';
            case PAYMENT.STATUS.PENDING:
                return 'bg-yellow-100 text-yellow-800 border-yellow-200';
            default:
                return 'bg-gray-100 text-gray-800 border-gray-200';
        }
    };

    const shouldShowRetryButton = () => {
        return payment.status === PAYMENT.STATUS.FAILED || payment.status === PAYMENT.STATUS.DECLINED;
    };

    const formatPaymentMethod = (method: string) => {
        // Capitalize and format payment method
        return method.split('_').map(word =>
            word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
        ).join(' ');
    };

    return (
        <div className="bg-white rounded-lg shadow-sm">
            {/* Header */}
            <div className="border-b border-gray-100 p-6">
                <div className="flex items-center space-x-3">
                    <div className="p-2 bg-[#f6d55c]/10 rounded-lg">
                        <CreditCard className="w-5 h-5 text-[#f6d55c]" />
                    </div>
                    <h2 className="text-lg font-semibold text-[#1a1a1a]">Payment Information</h2>
                </div>
            </div>

            <div className="p-6">
                {/* Payment Status */}
                <div className="mb-6">
                    <h3 className="text-sm font-medium text-[#718096] uppercase tracking-wide mb-3">
                        Payment Status
                    </h3>

                    <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                            {getPaymentStatusIcon(payment.status)}
                            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${getPaymentStatusBadge(payment.status)}`}>
                                {payment.status}
                            </span>
                        </div>

                        {shouldShowRetryButton() && (
                            <button
                                onClick={onRetryPayment}
                                className="flex items-center space-x-2 px-4 py-2 bg-[#f6d55c] hover:bg-[#e6c53f] text-[#1a1a1a] rounded-lg font-medium transition-colors duration-200"
                            >
                                <RefreshCw className="w-4 h-4" />
                                <span>Retry Payment</span>
                            </button>
                        )}
                    </div>

                    {/* Failure reason if applicable */}
                    {payment.failureReason && (
                        <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg">
                            <div className="flex items-start space-x-2">
                                <AlertTriangle className="w-4 h-4 text-red-600 mt-0.5 flex-shrink-0" />
                                <div>
                                    <p className="text-sm font-medium text-red-800">Payment Failed</p>
                                    <p className="text-sm text-red-700 mt-1">{payment.failureReason}</p>
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Payment Details */}
                <div className="mb-6">
                    <h3 className="text-sm font-medium text-[#718096] uppercase tracking-wide mb-4">
                        Payment Details
                    </h3>

                    <div className="space-y-4">
                        <div className="flex justify-between items-center py-2 border-b border-gray-50">
                            <span className="text-sm text-[#718096]">Payment Method:</span>
                            <span className="font-medium text-[#1a1a1a]">
                                {formatPaymentMethod(payment.paymentMethod)}
                            </span>
                        </div>

                        <div className="flex justify-between items-center py-2 border-b border-gray-50">
                            <span className="text-sm text-[#718096]">Transaction ID:</span>
                            <span className="font-mono text-sm text-[#1a1a1a] bg-gray-50 px-2 py-1 rounded">
                                {payment.transactionReference}
                            </span>
                        </div>

                        {payment.processedAt && (
                            <div className="flex justify-between items-center py-2 border-b border-gray-50">
                                <span className="text-sm text-[#718096]">Processed At:</span>
                                <span className="font-medium text-[#1a1a1a]">
                                    {new Date(payment.processedAt).toLocaleDateString('en-US', {
                                        year: 'numeric',
                                        month: 'long',
                                        day: 'numeric',
                                        hour: '2-digit',
                                        minute: '2-digit'
                                    })}
                                </span>
                            </div>
                        )}

                        <div className="flex justify-between items-center py-2">
                            <span className="text-sm text-[#718096]">Payment ID:</span>
                            <span className="font-mono text-sm text-[#1a1a1a] bg-gray-50 px-2 py-1 rounded">
                                #{payment.id}
                            </span>
                        </div>
                    </div>
                </div>

                {/* Payment Summary */}
                <div className="p-4 bg-gray-50 rounded-lg">
                    <h3 className="text-sm font-medium text-[#718096] uppercase tracking-wide mb-3">
                        Transaction Summary
                    </h3>

                    <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                            <span className="text-[#718096]">Status:</span>
                            <span className={`font-medium ${
                                payment.status === PAYMENT.STATUS.CONFIRMED ? 'text-green-600' :
                                    payment.status === PAYMENT.STATUS.FAILED || payment.status === PAYMENT.STATUS.DECLINED ? 'text-red-600' :
                                        payment.status === PAYMENT.STATUS.PENDING ? 'text-yellow-600' :
                                            'text-gray-600'
                            }`}>
                                {payment.status === PAYMENT.STATUS.CONFIRMED ? 'Successfully Processed' :
                                    payment.status === PAYMENT.STATUS.FAILED ? 'Transaction Failed' :
                                        payment.status === PAYMENT.STATUS.DECLINED ? 'Payment Declined' :
                                            payment.status === PAYMENT.STATUS.PENDING ? 'Processing...' :
                                                'Unknown Status'}
                            </span>
                        </div>

                        <div className="flex justify-between text-sm">
                            <span className="text-[#718096]">Security:</span>
                            <span className="text-green-600 font-medium">SSL Encrypted</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}