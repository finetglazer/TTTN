// fe/src/features/orders/components/CustomerInfo/CustomerInfoForm.tsx
'use client';

import { PLACEHOLDERS } from '@/core/config/constants';

// Types for Customer Information
export interface CustomerInfo {
    name: string;
    email: string;
    phone: string;
    address: string;
}

interface CustomerInfoFormProps {
    customerInfo: CustomerInfo;
    onCustomerInfoChange: (customerInfo: CustomerInfo) => void;
}

export default function CustomerInfoForm({
                                             customerInfo,
                                             onCustomerInfoChange
                                         }: CustomerInfoFormProps) {

    const handleFieldChange = (field: keyof CustomerInfo, value: string) => {
        onCustomerInfoChange({
            ...customerInfo,
            [field]: value
        });
    };

    return (
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
                        onChange={(e) => handleFieldChange('name', e.target.value)}
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
                        onChange={(e) => handleFieldChange('email', e.target.value)}
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
                        onChange={(e) => handleFieldChange('phone', e.target.value)}
                    />
                </div>
                <div>
                    <label className="form-label">Shipping Address *</label>
                    <input
                        type="text"
                        className="form-input"
                        placeholder={PLACEHOLDERS.ADDRESS}
                        value={customerInfo.address}
                        onChange={(e) => handleFieldChange('address', e.target.value)}
                        required
                    />
                </div>
            </div>
        </div>
    );
}