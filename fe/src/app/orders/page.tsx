import OrdersDashboard from '@/features/orders/views/orders-dashboard';
import AppLayout from '@/layouts/app-layout';

export default function OrdersPage() {
    return (
        <AppLayout>
            <OrdersDashboard />
        </AppLayout>
    );
}