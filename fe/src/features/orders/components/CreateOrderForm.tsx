// fe/src/features/orders/components/CreateOrderForm.tsx (REFACTORED)
'use client';

import { useState } from 'react';
import { useCreateOrder } from '@/features/orders/hooks/orders.hooks';
import { CreateOrderRequest, OrderItem } from '@/features/orders/types/orders.create.types';

// Import the new divided components
import CustomerInfoForm, { CustomerInfo } from '@/features/orders/components/CustomInfo/CustomerInfoForm';
import OrderItemsManager from '@/features/orders/components/OrderItems/OrderItemsManager';
import OrderSummary, { calculateOrderTotals } from '@/features/orders/components/OrderSummary/OrderSummary';
import OrderSuccessNotification from './OrderSuccessNotification';

export default function CreateOrderForm() {
    // State management
    const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
    const [customerInfo, setCustomerInfo] = useState<CustomerInfo>({
        name: '',
        email: '',
        phone: '',
        address: ''
    });
    const [showSuccessNotification, setShowSuccessNotification] = useState(false);

    // Hooks
    const createOrderMutation = useCreateOrder();

    // Calculate totals
    const { total } = calculateOrderTotals(orderItems);

    // Handle form submission
    const handleSubmit = (e?: React.FormEvent) => {
        if (e) e.preventDefault();

        // Validate required fields
        if (!customerInfo.name || !customerInfo.email || !customerInfo.address) {
            alert('Please fill in all required customer information.');
            return;
        }

        if (orderItems.length === 0) {
            alert('Please add at least one item to the order.');
            return;
        }

        // Prepare order data for API
        const orderDescription = orderItems
            .map(item => `${item.name} (x${item.quantity}) - $${(item.price * item.quantity).toFixed(2)}`)
            .join(', ');

        // Generate random userId for demo purposes
        const userId = 'user_' + Math.random().toString(36).substring(2, 15);

        const orderData: CreateOrderRequest = {
            userId,
            userEmail: customerInfo.email,
            userName: customerInfo.name,
            orderDescription,
            totalAmount: total,
            shippingAddress: customerInfo.address,
        };

        // Submit order
        createOrderMutation.mutate(orderData, {
            onSuccess: (data) => {
                setShowSuccessNotification(true);
                // Reset form
                setOrderItems([]);
                setCustomerInfo({
                    name: '',
                    email: '',
                    phone: '',
                    address: ''
                });
                console.log('Order created successfully:', data);
            },
            onError: (error) => {
                alert('Failed to create order. Please try again.');
                console.error('Order creation failed:', error);
            },
        });
    };

    return (
        <>
            <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Left Side - Form Components */}
                <div className="lg:col-span-2 space-y-8">
                    <CustomerInfoForm
                        customerInfo={customerInfo}
                        onCustomerInfoChange={setCustomerInfo}
                    />

                    <OrderItemsManager
                        orderItems={orderItems}
                        onOrderItemsChange={setOrderItems}
                    />
                </div>

                {/* Right Side - Order Summary */}
                <OrderSummary
                    orderItems={orderItems}
                    isSubmitting={createOrderMutation.isPending}
                    onSubmit={handleSubmit}
                />
            </form>

            {/* Success Notification */}
            <OrderSuccessNotification
                isOpen={showSuccessNotification}
                onClose={() => setShowSuccessNotification(false)}
            />
        </>
    );
}
