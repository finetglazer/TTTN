'use client';

import { useState } from 'react';
import { ORDER, PLACEHOLDERS, MESSAGES } from '@/core/config/constants';
import { useCreateOrder } from '@/features/orders/hooks/orders.hooks';
import { CreateOrderRequest, OrderItem, NewItem} from '@/features/orders/types/orders.create.types';
import OrderSuccessNotification from './OrderSuccessNotification';



export default function CreateOrderForm() {
    const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
    const [customerInfo, setCustomerInfo] = useState({
        name: '',
        email: '',
        phone: '',
        address: ''
    });
    const [newItem, setNewItem] = useState<NewItem>({
        name: '',
        price: '',
        quantity: ORDER.DEFAULT_QUANTITY
    });

    // Add notification state
    const [showSuccessNotification, setShowSuccessNotification] = useState(false);

    const createOrderMutation = useCreateOrder();

    const addItem = () => {
        if (newItem.name && newItem.price) {
            const item: OrderItem = {
                id: Date.now().toString(),
                name: newItem.name,
                price: parseFloat(newItem.price),
                quantity: newItem.quantity
            };
            setOrderItems([...orderItems, item]);
            setNewItem({ name: '', price: '', quantity: ORDER.DEFAULT_QUANTITY });
        }
    };

    const removeItem = (id: string) => {
        setOrderItems(orderItems.filter(item => item.id !== id));
    };

    const subtotal = orderItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const tax = subtotal * ORDER.TAX_RATE;
    const total = subtotal + tax;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        // Prepare order data for API
        const orderDescription = orderItems
            .map(item => `${item.name} (x${item.quantity}) - $${(item.price * item.quantity).toFixed(2)}`)
            .join(', ');
        // random userId for demo purposes
        const userId = 'user_' + Math.random().toString(36).substring(2, 15);

        const orderData: CreateOrderRequest = {
            userId,
            userEmail: customerInfo.email,
            userName: customerInfo.name,
            orderDescription,
            totalAmount: total,
            shippingAddress: customerInfo.address,
        };

        createOrderMutation.mutate(orderData, {
            onSuccess: (data) => {
                // Replace alert with notification
                setShowSuccessNotification(true);
                // Reset form
                setOrderItems([]);
                setCustomerInfo({ name: '', email: '', phone: '', address: '' });
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
                {/* Order Form - Left Side */}
                <div className="lg:col-span-2 space-y-8">
                    {/* Customer Information */}
                    <div className="card">
                        <h2 className="text-xl font-semibold text-[#1a1a1a] mb-6 pb-2 border-b-2 border-[#f6d55c]">
                            Customer Information
                        </h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="form-label">Full Name *</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    placeholder={PLACEHOLDERS.CUSTOMER_NAME}
                                    value={customerInfo.name}
                                    onChange={(e) => setCustomerInfo({...customerInfo, name: e.target.value})}
                                    required
                                />
                            </div>
                            <div>
                                <label className="form-label">Email *</label>
                                <input
                                    type="email"
                                    className="form-input"
                                    placeholder={PLACEHOLDERS.EMAIL}
                                    value={customerInfo.email}
                                    onChange={(e) => setCustomerInfo({...customerInfo, email: e.target.value})}
                                    required
                                />
                            </div>
                            <div>
                                <label className="form-label">Phone Number</label>
                                <input
                                    type="tel"
                                    className="form-input"
                                    placeholder={PLACEHOLDERS.PHONE}
                                    value={customerInfo.phone}
                                    onChange={(e) => setCustomerInfo({...customerInfo, phone: e.target.value})}
                                />
                            </div>
                            <div>
                                <label className="form-label">Shipping Address *</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    placeholder={PLACEHOLDERS.ADDRESS}
                                    value={customerInfo.address}
                                    onChange={(e) => setCustomerInfo({...customerInfo, address: e.target.value})}
                                    required
                                />
                            </div>
                        </div>
                    </div>

                    {/* Order Items */}
                    <div className="card">
                        <h2 className="text-xl font-semibold text-[#1a1a1a] mb-6 pb-2 border-b-2 border-[#f6d55c]">
                            Order Items
                        </h2>

                        {/* Add New Item */}
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6 p-4 bg-[#f7fafc] rounded-lg">
                            <div>
                                <label className="form-label">Item Name</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    placeholder={PLACEHOLDERS.PRODUCT_NAME}
                                    value={newItem.name}
                                    onChange={(e) => setNewItem({...newItem, name: e.target.value})}
                                />
                            </div>
                            <div>
                                <label className="form-label">Price ($)</label>
                                <input
                                    type="number"
                                    step={ORDER.PRICE_STEP}
                                    className="form-input"
                                    placeholder={PLACEHOLDERS.PRICE}
                                    value={newItem.price}
                                    onChange={(e) => setNewItem({...newItem, price: e.target.value})}
                                />
                            </div>
                            <div>
                                <label className="form-label">Quantity</label>
                                <input
                                    type="number"
                                    min={ORDER.MIN_QUANTITY.toString()}
                                    className="form-input"
                                    value={newItem.quantity}
                                    onChange={(e) => setNewItem({...newItem, quantity: parseInt(e.target.value)})}
                                />
                            </div>
                            <div className="flex items-end">
                                <button
                                    type="button"
                                    onClick={addItem}
                                    className="btn-primary w-full"
                                >
                                    Add Item
                                </button>
                            </div>
                        </div>

                        {/* Items List */}
                        {orderItems.length === 0 ? (
                            <p className="text-[#718096] text-center py-8">No items added yet</p>
                        ) : (
                            <div className="space-y-3">
                                {orderItems.map((item) => (
                                    <div key={item.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                                        <div className="flex-1">
                                            <h4 className="font-medium text-[#2d3748]">{item.name}</h4>
                                            <p className="text-sm text-[#718096]">
                                                ${item.price.toFixed(2)} × {item.quantity}
                                            </p>
                                        </div>
                                        <div className="flex items-center space-x-3">
                                            <span className="font-semibold text-[#1a1a1a]">
                                                ${(item.price * item.quantity).toFixed(2)}
                                            </span>
                                            <button
                                                type="button"
                                                onClick={() => removeItem(item.id)}
                                                className="text-red-500 hover:text-red-700 text-sm"
                                            >
                                                Remove
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Order Summary - Right Side */}
                <div className="lg:col-span-1">
                    <div className="card sticky top-8">
                        <h2 className="text-xl font-semibold text-[#1a1a1a] mb-6 pb-2 border-b-2 border-[#f6d55c]">
                            Order Summary
                        </h2>

                        {orderItems.length === 0 ? (
                            <p className="text-[#718096] text-center py-8">No items added yet</p>
                        ) : (
                            <>
                                {/* Items List */}
                                <div className="space-y-3 mb-6">
                                    {orderItems.map((item) => (
                                        <div key={item.id} className="flex justify-between text-sm">
                                            <span className="text-[#2d3748]">{item.name} × {item.quantity}</span>
                                            <span className="font-medium">${(item.price * item.quantity).toFixed(2)}</span>
                                        </div>
                                    ))}
                                </div>

                                {/* Calculations */}
                                <div className="border-t pt-4 space-y-2">
                                    <div className="flex justify-between text-sm">
                                        <span className="text-[#718096]">Subtotal:</span>
                                        <span>${subtotal.toFixed(2)}</span>
                                    </div>
                                    <div className="flex justify-between text-sm">
                                        <span className="text-[#718096]">Tax (10%):</span>
                                        <span>${tax.toFixed(2)}</span>
                                    </div>
                                    <div className="flex justify-between text-lg font-bold text-[#f6d55c] pt-2 border-t">
                                        <span>Total:</span>
                                        <span>${total.toFixed(2)}</span>
                                    </div>
                                </div>
                            </>
                        )}

                        {/* Submit Button */}
                        <button
                            type="submit"
                            disabled={
                                orderItems.length === 0 ||
                                !customerInfo.name ||
                                !customerInfo.email ||
                                !customerInfo.address ||
                                createOrderMutation.isPending
                            }
                            className="w-full btn-primary mt-6 h-14 text-lg disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {createOrderMutation.isPending ? 'Creating Order...' : 'Create Order'}
                        </button>

                        {/* Loading/Error States */}
                        {createOrderMutation.isPending && (
                            <p className="text-sm text-[#718096] text-center mt-2">
                                Please wait while we process your order...
                            </p>
                        )}
                    </div>
                </div>
            </form>

            {/* Success Notification */}
            <OrderSuccessNotification
                isOpen={showSuccessNotification}
                onClose={() => setShowSuccessNotification(false)}
            />
        </>
    );
}