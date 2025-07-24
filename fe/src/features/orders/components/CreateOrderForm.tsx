'use client';

import { useState } from 'react';
import { ORDER, PLACEHOLDERS, MESSAGES } from '@/core/config/constants';
import { useCreateOrder } from '@/features/orders/hooks/orders.hooks';
import { CreateOrderRequest, OrderItem, NewItem} from '@/features/orders/types/orders.create.types';
import OrderSuccessNotification from './OrderSuccessNotification';
import { createOrderRequestSchema } from '@/features/orders/validations/orders.schema';
import { z } from 'zod';

// Type for validation errors
type ValidationErrors = {
    [key: string]: string;
};

// Error message component
const ErrorMessage = ({ message }: { message: string }) => (
    <p className="text-red-500 text-sm mt-1 flex items-center">
        <svg className="w-4 h-4 mr-1 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
        </svg>
        {message}
    </p>
);

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

    // Add validation errors state
    const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});
    const [showSuccessNotification, setShowSuccessNotification] = useState(false);

    const createOrderMutation = useCreateOrder();

    // Clear error for specific field
    const clearFieldError = (fieldName: string) => {
        if (validationErrors[fieldName]) {
            setValidationErrors(prev => {
                const newErrors = { ...prev };
                delete newErrors[fieldName];
                return newErrors;
            });
        }
    };

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

        // Clear previous validation errors
        setValidationErrors({});

        // Prepare order data for validation
        const orderDescription = orderItems
            .map(item => `${item.name} (x${item.quantity}) - $${(item.price * item.quantity).toFixed(2)}`)
            .join(', ');

        const userId = 'user_' + Math.random().toString(36).substring(2, 15);

        const orderData: CreateOrderRequest = {
            userId,
            userEmail: customerInfo.email,
            userName: customerInfo.name,
            orderDescription,
            totalAmount: total,
            shippingAddress: customerInfo.address,
        };

        // Validate data before sending
        try {
            createOrderRequestSchema.parse(orderData);
        } catch (error) {
            if (error instanceof z.ZodError) {
                const newErrors: ValidationErrors = {};
                error.issues.forEach((err: z.ZodIssue) => {
                    const fieldName = err.path[0] as string;
                    newErrors[fieldName] = err.message;
                });
                setValidationErrors(newErrors);
                return; // Don't submit if validation fails
            }
        }

        // Additional custom validation for order items
        if (orderItems.length === 0) {
            setValidationErrors({ orderItems: 'Please add at least one item to your order' });
            return;
        }

        createOrderMutation.mutate(orderData, {
            onSuccess: (data) => {
                setShowSuccessNotification(true);
                // Reset form and clear errors
                setOrderItems([]);
                setCustomerInfo({ name: '', email: '', phone: '', address: '' });
                setValidationErrors({});
                console.log('Order created successfully:', data);
            },
            onError: (error: any) => {
                console.error('Order creation failed:', error);

                // Handle different types of errors
                if (error instanceof z.ZodError) {
                    // Handle Zod validation errors
                    const newErrors: ValidationErrors = {};
                    error.issues.forEach((err: z.ZodIssue) => {
                        const fieldName = err.path[0] as string;
                        newErrors[fieldName] = err.message;
                    });
                    setValidationErrors(newErrors);
                } else if (error?.response?.data?.message) {
                    // Handle API errors with specific messages
                    setValidationErrors({
                        general: error.response.data.message
                    });
                } else {
                    // Handle generic errors
                    setValidationErrors({
                        general: 'Failed to create order. Please check your information and try again.'
                    });
                }
            },
        });
    };

    return (
        <>
            <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Order Form - Left Side */}
                <div className="lg:col-span-2 space-y-8">
                    {/* General Error Message */}
                    {validationErrors.general && (
                        <div className="bg-red-50 border border-red-200 rounded-md p-4">
                            <ErrorMessage message={validationErrors.general} />
                        </div>
                    )}

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
                                    className={`form-input ${validationErrors.userName ? 'border-red-500 focus:border-red-500' : ''}`}
                                    placeholder={PLACEHOLDERS.CUSTOMER_NAME}
                                    value={customerInfo.name}
                                    onChange={(e) => {
                                        setCustomerInfo({...customerInfo, name: e.target.value});
                                        clearFieldError('userName');
                                    }}
                                    required
                                />
                                {validationErrors.userName && <ErrorMessage message={validationErrors.userName} />}
                            </div>
                            <div>
                                <label className="form-label">Email *</label>
                                <input
                                    type="email"
                                    className={`form-input ${validationErrors.userEmail ? 'border-red-500 focus:border-red-500' : ''}`}
                                    placeholder={PLACEHOLDERS.EMAIL}
                                    value={customerInfo.email}
                                    onChange={(e) => {
                                        setCustomerInfo({...customerInfo, email: e.target.value});
                                        clearFieldError('userEmail');
                                    }}
                                    required
                                />
                                {validationErrors.userEmail && <ErrorMessage message={validationErrors.userEmail} />}
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
                                    className={`form-input ${validationErrors.shippingAddress ? 'border-red-500 focus:border-red-500' : ''}`}
                                    placeholder={PLACEHOLDERS.ADDRESS}
                                    value={customerInfo.address}
                                    onChange={(e) => {
                                        setCustomerInfo({...customerInfo, address: e.target.value});
                                        clearFieldError('shippingAddress');
                                    }}
                                    required
                                />
                                {validationErrors.shippingAddress && <ErrorMessage message={validationErrors.shippingAddress} />}
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
                                <label className="form-label">Price</label>
                                <input
                                    type="number"
                                    step={ORDER.PRICE_STEP}
                                    min="0"
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
                                    min={ORDER.MIN_QUANTITY}
                                    className="form-input"
                                    value={newItem.quantity}
                                    onChange={(e) => setNewItem({...newItem, quantity: parseInt(e.target.value) || ORDER.DEFAULT_QUANTITY})}
                                />
                            </div>
                            <div className="flex items-end">
                                <button
                                    type="button"
                                    onClick={addItem}
                                    className="btn-secondary w-full h-12"
                                    disabled={!newItem.name || !newItem.price}
                                >
                                    Add Item
                                </button>
                            </div>
                        </div>

                        {/* Order Items Error */}
                        {validationErrors.orderItems && <ErrorMessage message={validationErrors.orderItems} />}

                        {/* Current Items */}
                        {orderItems.length > 0 && (
                            <div className="space-y-3">
                                {orderItems.map((item) => (
                                    <div key={item.id} className="flex justify-between items-center p-3 bg-[#f7fafc] rounded-lg">
                                        <div>
                                            <span className="font-medium text-[#2d3748]">{item.name}</span>
                                            <span className="text-sm text-[#718096] ml-2">
                                                ${item.price.toFixed(2)} × {item.quantity}
                                            </span>
                                        </div>
                                        <div className="flex items-center gap-3">
                                            <span className="font-semibold text-[#f6d55c]">
                                                ${(item.price * item.quantity).toFixed(2)}
                                            </span>
                                            <button
                                                type="button"
                                                onClick={() => removeItem(item.id)}
                                                className="text-red-500 hover:text-red-700 p-1"
                                                title="Remove item"
                                            >
                                                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                                    <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                                                </svg>
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

                        {validationErrors.totalAmount && (
                            <div className="mt-2">
                                <ErrorMessage message={validationErrors.totalAmount} />
                            </div>
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