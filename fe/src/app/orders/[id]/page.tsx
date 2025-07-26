// fe/src/app/orders/[id]/page.tsx
import OrderDetailsPage from '@/features/orders/views/order-details';
import AppLayout from '@/layouts/app-layout';
import { QueryProvider } from '@/core/providers/query-provider';

interface OrderDetailsPageProps {
    params: {
        id: string;
    };
}

// Making the Page component async to align with modern Next.js patterns for handling props in Server Components.
export default async function OrderDetailsAppPage({ params }: OrderDetailsPageProps) {
    // As per Next.js 15+, the params object must be awaited in async Server Components.
    const resolvedParams = await params;

    return (
        <AppLayout>
            <QueryProvider>
                {/* Pass the resolved id to the orderId prop. */}
                <OrderDetailsPage orderId={resolvedParams.id} />
            </QueryProvider>
        </AppLayout>
    );
}

// Generate metadata for the page
export async function generateMetadata({ params }: OrderDetailsPageProps) {
    // Awaiting params to resolve its properties before use.
    const resolvedParams = await params;
    return {
        // Updated to use the resolved id for generating the title and description.
        title: `Order #${resolvedParams.id} - Order Management Portal`,
        description: `View details for order #${resolvedParams.id}`,
    };
}
