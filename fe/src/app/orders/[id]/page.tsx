// fe/src/app/orders/[orderId]/page.tsx
import OrderDetailsPage from '@/features/orders/views/order-details';
import AppLayout from '@/layouts/app-layout';
import { QueryProvider } from '@/core/providers/query-provider';

interface OrderDetailsPageProps {
    params: {
        // Corrected to 'id' to match the dynamic route parameter from the folder structure ([id]).
        id: string;
    };
}

export default function OrderDetailsAppPage({ params }: OrderDetailsPageProps) {
    return (
        <AppLayout>
            <QueryProvider>
                {/* Pass params.id to the orderId prop. */}
                <OrderDetailsPage orderId={params.id} />
            </QueryProvider>
        </AppLayout>
    );
}

// Generate metadata for the page
export async function generateMetadata({ params }: OrderDetailsPageProps) {
    return {
        // Updated to use params.id for generating the title.
        title: `Order #${params.id} - Order Management Portal`,
        description: `View details for order #${params.id}`,
    };
}
