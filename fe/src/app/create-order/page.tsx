import OrderCreate from '@/features/orders/views/create-order';
import AppLayout from '@/layouts/app-layout';
import {QueryProvider} from '@/core/providers/query-provider';

export default function OrdersPage() {
    return (
        <AppLayout>
            <QueryProvider>
                <OrderCreate/>
            </QueryProvider>
        </AppLayout>
    );
}