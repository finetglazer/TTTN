'use client';

import { useState } from 'react';
import Header from "@/components/layout/Header";

interface OrderItem {
    id: string;
    name: string;
    price: number;
    quantity: number;
}

export default function CreateOrderPage() {
    const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
    const [customerInfo, setCustomerInfo] = useState({
        name: '',
        email: '',
        phone: '',
        address: ''
    });
    const [newItem, setNewItem] = useState({
        name: '',
        price: '',
        quantity: 1
    });

    const addItem = () => {
        if (newItem.name && newItem.price) {
            const item: OrderItem = {
                id: Date.now().toString(),
                name: newItem.name,
                price: parseFloat(newItem.price),
                quantity: newItem.quantity
            };
            setOrderItems([...orderItems, item]);
            setNewItem({ name: '', price: '', quantity: 1 });
        }
    };

    const removeItem = (id: string) => {
        setOrderItems(orderItems.filter(item => item.id !== id));
    };

    const subtotal = orderItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const tax = subtotal * 0.1; // 10% tax
    const total = subtotal + tax;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        // Mock order submission
        alert('Order created successfully!');
    };

    return (
        <div className="min-h-screen bg-[#f7fafc]">
            <Header />

            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-[#1a1a1a] mb-2">Create New Order</h1>
                    <p className="text-[#718096]">Fill in the details below to create a new order</p>
                </div>

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
                                        placeholder="Enter customer name"
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
                                        placeholder="customer@example.com"
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
                                        placeholder="+1 (555) 123-4567"
                                        value={customerInfo.phone}
                                        onChange={(e) => setCustomerInfo({...customerInfo, phone: e.target.value})}
                                    />
                                </div>
                                <div>
                                    <label className="form-label">Shipping Address *</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        placeholder="123 Main St, City, State"
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
                                        placeholder="Product name"
                                        value={newItem.name}
                                        onChange={(e) => setNewItem({...newItem, name: e.target.value})}
                                    />
                                </div>
                                <div>
                                    <label className="form-label">Price ($)</label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        className="form-input"
                                        placeholder="0.00"
                                        value={newItem.price}
                                        onChange={(e) => setNewItem({...newItem, price: e.target.value})}
                                    />
                                </div>
                                <div>
                                    <label className="form-label">Quantity</label>
                                    <input
                                        type="number"
                                        min="1"
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
                            {orderItems.length > 0 && (
                                <div className="space-y-3">
                                    {orderItems.map((item) => (
                                        <div key={item.id} className="flex items-center justify-between p-4 bg-white border rounded-lg">
                                            <div className="flex-1">
                                                <h4 className="font-medium text-[#1a1a1a]">{item.name}</h4>
                                                <p className="text-sm text-[#718096]">
                                                    ${item.price.toFixed(2)} × {item.quantity} = ${(item.price * item.quantity).toFixed(2)}
                                                </p>
                                            </div>
                                            <button
                                                type="button"
                                                onClick={() => removeItem(item.id)}
                                                className="text-[#f56565] hover:text-red-700 ml-4"
                                            >
                                                Remove
                                            </button>
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
                                disabled={orderItems.length === 0 || !customerInfo.name || !customerInfo.email}
                                className="w-full btn-primary mt-6 h-14 text-lg disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Create Order
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}