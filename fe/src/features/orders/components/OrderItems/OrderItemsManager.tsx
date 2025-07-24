// fe/src/features/orders/components/OrderItems/OrderItemsManager.tsx
'use client';

import { useState } from 'react';
import { ORDER, PLACEHOLDERS } from '@/core/config/constants';
import { OrderItem, NewItem } from '@/features/orders/types/orders.create.types';

interface OrderItemsManagerProps {
    orderItems: OrderItem[];
    onOrderItemsChange: (items: OrderItem[]) => void;
}

// Sub-component for adding new items
function AddItemForm({
                         newItem,
                         onNewItemChange,
                         onAddItem
                     }: {
    newItem: NewItem;
    onNewItemChange: (item: NewItem) => void;
    onAddItem: () => void;
}) {
    const handleFieldChange = (field: keyof NewItem, value: string | number) => {
        onNewItemChange({
            ...newItem,
            [field]: value
        });
    };

    return (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6 p-4 bg-[#f7fafc] rounded-lg">
            <div>
                <label className="form-label">Item Name</label>
                <input
                    type="text"
                    className="form-input"
                    placeholder={PLACEHOLDERS.PRODUCT_NAME}
                    value={newItem.name}
                    onChange={(e) => handleFieldChange('name', e.target.value)}
                />
            </div>
            <div>
                <label className="form-label">Price</label>
                <input
                    type="number"
                    className="form-input"
                    placeholder={PLACEHOLDERS.PRICE}
                    value={newItem.price}
                    onChange={(e) => handleFieldChange('price', e.target.value)}
                    step={ORDER.PRICE_STEP}
                    min="0"
                />
            </div>
            <div>
                <label className="form-label">Quantity</label>
                <input
                    type="number"
                    className="form-input"
                    value={newItem.quantity}
                    onChange={(e) => handleFieldChange('quantity', parseInt(e.target.value) || ORDER.DEFAULT_QUANTITY)}
                    min={ORDER.MIN_QUANTITY}
                />
            </div>
            <div className="flex items-end">
                <button
                    type="button"
                    onClick={onAddItem}
                    className="btn-secondary w-full"
                    disabled={!newItem.name || !newItem.price}
                >
                    Add Item
                </button>
            </div>
        </div>
    );
}

// Sub-component for individual order item row
function OrderItemRow({
                          item,
                          onRemove
                      }: {
    item: OrderItem;
    onRemove: (id: string) => void;
}) {
    return (
        <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
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
                    onClick={() => onRemove(item.id)}
                    className="text-red-500 hover:text-red-700 text-sm"
                >
                    Remove
                </button>
            </div>
        </div>
    );
}

// Sub-component for items list
function ItemsList({
                       items,
                       onRemoveItem
                   }: {
    items: OrderItem[];
    onRemoveItem: (id: string) => void;
}) {
    if (items.length === 0) {
        return (
            <p className="text-[#718096] text-center py-8">No items added yet</p>
        );
    }

    return (
        <div className="space-y-3">
            {items.map((item) => (
                <OrderItemRow
                    key={item.id}
                    item={item}
                    onRemove={onRemoveItem}
                />
            ))}
        </div>
    );
}

// Main OrderItemsManager component
export default function OrderItemsManager({
                                              orderItems,
                                              onOrderItemsChange
                                          }: OrderItemsManagerProps) {
    const [newItem, setNewItem] = useState<NewItem>({
        name: '',
        price: '',
        quantity: ORDER.DEFAULT_QUANTITY
    });

    const addItem = () => {
        if (newItem.name && newItem.price) {
            const item: OrderItem = {
                id: Date.now().toString(),
                name: newItem.name,
                price: parseFloat(newItem.price),
                quantity: newItem.quantity
            };
            onOrderItemsChange([...orderItems, item]);
            setNewItem({
                name: '',
                price: '',
                quantity: ORDER.DEFAULT_QUANTITY
            });
        }
    };

    const removeItem = (id: string) => {
        onOrderItemsChange(orderItems.filter(item => item.id !== id));
    };

    return (
        <div className="card">
            <h2 className="text-xl font-semibold text-[#1a1a1a] mb-6 pb-2 border-b-2 border-[#f6d55c]">
                Order Items
            </h2>

            <AddItemForm
                newItem={newItem}
                onNewItemChange={setNewItem}
                onAddItem={addItem}
            />

            <div>
                <h3 className="text-lg font-medium text-[#2d3748] mb-4">
                    Items ({orderItems.length})
                </h3>
                <ItemsList
                    items={orderItems}
                    onRemoveItem={removeItem}
                />
            </div>
        </div>
    );
}