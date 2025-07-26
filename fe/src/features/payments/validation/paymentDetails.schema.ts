// fe/src/features/payments/validation/paymentDetails.schema.ts
import {z} from "zod";

const PaymentDetailSchema = z.object({
    id: z.number(),
    status: z.string(),
    paymentMethod: z.string(),
    transactionReference: z.string(),
    processedAt: z.string(),
    failureReason: z.string().nullable(),
});

const PaymentDetailResponseSchema = z.object({
    status: z.number(),
    msg: z.string(),
    data: PaymentDetailSchema.nullable(), // 🎯 Changed: Allow null when payment doesn't exist yet
});

export {
    PaymentDetailSchema,
    PaymentDetailResponseSchema
}