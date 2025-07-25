


export interface PaymentDetail {
    id: number;
    paymentMethod: string;
    status: string;
    transactionReference: string;
    processedAt: string;
    failureReason: string | null;
}