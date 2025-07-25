import {z} from "zod";

const PaymentDetailSchema = z.object({
    id: z.number(),
    status: z.string(),
    paymentMethod: z.string(),
    transactionReference: z.string(),
    processedAt: z.string(),
    failureReason: z.string().nullable(),
    // Exclude: authToken (sensitive), sagaId, retryCount, lastRetryAt, externalTransactionId (internal)
});

const PaymentDetailResponseSchema = z.object({
    status: z.number(),
    msg: z.string(),
    data: PaymentDetailSchema,
});

export {
    PaymentDetailSchema,
    PaymentDetailResponseSchema
}