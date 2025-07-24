import { z } from 'zod';
import { ORDER } from '@/core/config/constants';

// Create a union type from the ORDER.STATUS values
const orderStatusSchema = z.enum([
    ORDER.STATUS.CREATED,
    ORDER.STATUS.CONFIRMED,
    ORDER.STATUS.DELIVERED,
    ORDER.STATUS.CANCELLED
] as const);

// Schema for individual order item
const ordersDashboardDisplaySchema = z.object({
    // orderId = ORD_ + string
    orderId: z.string().transform(id => `ORD_${id}`),
    orderDescription: z.string(),
    userName: z.string(),
    // Handle BigDecimal -> number conversion
    totalAmount: z.union([
        z.number(),
        z.string().transform((val) => parseFloat(val))
    ]),
    // Handle LocalDateTime format: "2025-07-18 21:01:13.699945"
    createdAt: z.string().transform((dateString) => {
        // If it's already in ISO format, return as-is
        if (dateString.includes('T')) {
            return dateString;
        }
        // Convert "2025-07-18 21:01:13.699945" to ISO format
        // Replace space with 'T' and ensure it ends with 'Z' or timezone info
        const isoString = dateString.replace(' ', 'T');
        return isoString.includes('Z') || isoString.includes('+') || isoString.includes('-', 10)
            ? isoString
            : `${isoString}Z`;
    }),
    orderStatus: orderStatusSchema
});

// Schema for the complete API response
const getAllOrdersResponseSchema = z.object({
    status: z.number(),
    msg: z.string(),
    data: z.array(ordersDashboardDisplaySchema)
});

// Export the schemas for use in API calls
export {
    ordersDashboardDisplaySchema,
    getAllOrdersResponseSchema,
    orderStatusSchema
};