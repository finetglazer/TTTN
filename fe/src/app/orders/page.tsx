import OrdersDashboard from '@/features/orders/views/orders-dashboard';
import AppLayout from '@/layouts/app-layout';
import {QueryProvider} from "@/core/providers/query-provider";

export default function OrdersPage() {
    return (
        <AppLayout>
            <QueryProvider>
            <OrdersDashboard />
            </QueryProvider>
        </AppLayout>
    );
}