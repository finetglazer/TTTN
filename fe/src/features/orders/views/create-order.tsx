'use client';

import CreateOrderForm from '@/features/orders/components/CreateOrderForm';

export default function CreateOrderPage() {
    return (
        <div className="min-h-screen bg-[#f7fafc]">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-[#1a1a1a] mb-2">Create New Order</h1>
                    <p className="text-[#718096]">Fill in the details below to create a new order</p>
                </div>
                <CreateOrderForm />
            </div>
        </div>
    );
}