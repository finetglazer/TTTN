// fe/src/app/orders/[id]/page.tsx
import OrderDetailsPage from '@/features/orders/views/order-details';
import AppLayout from '@/layouts/app-layout';
import { QueryProvider } from '@/core/providers/query-provider';

// The type should now reflect that params is a Promise.
// While updating @types/next should handle this implicitly,
// being explicit can help clarify the new pattern.
interface OrderDetailsPageProps {
    params: Promise<{ id: string; }>; // Or simply { params: { id: string } } after deps update
}

export default async function OrderDetailsAppPage({ params }: OrderDetailsPageProps) {
    // Await the params promise to get the actual values
    const { id } = await params;

    return (
        <AppLayout>
            <QueryProvider>
                <OrderDetailsPage orderId={id} />
            </QueryProvider>
        </AppLayout>
    );
}

// Generate metadata for the page
export async function generateMetadata({ params }: OrderDetailsPageProps) {
    // Await the params promise here as well
    const { id } = await params;
    return {
        title: `Order #${id} - Order Management Portal`,
        description: `View details for order #${id}`,
    };
}