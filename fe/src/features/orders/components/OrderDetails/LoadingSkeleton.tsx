// fe/src/features/orders/components/OrderDetails/LoadingSkeleton.tsx
'use client';

export function LoadingSkeleton() {
    return (
        <div className="min-h-screen bg-[#f7fafc]">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Header Skeleton */}
                <div className="bg-white rounded-lg shadow-sm p-6 mb-8 animate-pulse">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4">
                            <div className="flex items-center space-x-2">
                                <div className="w-5 h-5 bg-gray-300 rounded"></div>
                                <div className="w-12 h-4 bg-gray-300 rounded"></div>
                            </div>
                            <div className="border-l border-gray-300 pl-4">
                                <div className="w-32 h-6 bg-gray-300 rounded mb-2"></div>
                                <div className="w-48 h-4 bg-gray-200 rounded"></div>
                            </div>
                        </div>
                        <div className="w-24 h-8 bg-gray-300 rounded"></div>
                    </div>

                    <div className="mt-4 pt-4 border-t border-gray-100">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                            <div className="w-40 h-4 bg-gray-200 rounded"></div>
                            <div className="w-36 h-4 bg-gray-200 rounded"></div>
                            <div className="w-28 h-4 bg-gray-200 rounded"></div>
                        </div>
                    </div>
                </div>

                {/* Timeline Skeleton */}
                <div className="bg-white rounded-lg shadow-sm p-6 mb-8 animate-pulse">
                    <div className="w-32 h-5 bg-gray-300 rounded mb-6"></div>

                    <div className="space-y-8">
                        {[1, 2, 3].map((item) => (
                            <div key={item} className="flex items-start">
                                <div className="w-12 h-12 bg-gray-300 rounded-full"></div>
                                <div className="ml-4 flex-1">
                                    <div className="flex items-center justify-between">
                                        <div className="w-20 h-4 bg-gray-300 rounded"></div>
                                        <div className="w-32 h-3 bg-gray-200 rounded"></div>
                                    </div>
                                    <div className="w-40 h-3 bg-gray-200 rounded mt-2"></div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Two-column layout skeleton */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* Order Details Card Skeleton */}
                    <div className="bg-white rounded-lg shadow-sm animate-pulse">
                        <div className="border-b border-gray-100 p-6">
                            <div className="flex items-center space-x-3">
                                <div className="w-9 h-9 bg-gray-300 rounded-lg"></div>
                                <div className="w-28 h-5 bg-gray-300 rounded"></div>
                            </div>
                        </div>

                        <div className="p-6">
                            {/* Items skeleton */}
                            <div className="mb-6">
                                <div className="w-24 h-4 bg-gray-300 rounded mb-4"></div>
                                <div className="space-y-4">
                                    {[1, 2, 3].map((item) => (
                                        <div key={item} className="flex justify-between py-3">
                                            <div>
                                                <div className="w-32 h-4 bg-gray-300 rounded mb-1"></div>
                                                <div className="w-20 h-3 bg-gray-200 rounded"></div>
                                            </div>
                                            <div className="w-16 h-4 bg-gray-300 rounded"></div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Pricing skeleton */}
                            <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                                <div className="w-28 h-4 bg-gray-300 rounded mb-3"></div>
                                <div className="space-y-2">
                                    {[1, 2, 3, 4].map((item) => (
                                        <div key={item} className="flex justify-between">
                                            <div className="w-16 h-3 bg-gray-200 rounded"></div>
                                            <div className="w-12 h-3 bg-gray-200 rounded"></div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Customer info skeleton */}
                            <div>
                                <div className="w-36 h-4 bg-gray-300 rounded mb-4"></div>
                                <div className="space-y-3">
                                    {[1, 2, 3].map((item) => (
                                        <div key={item} className="flex items-center space-x-3">
                                            <div className="w-4 h-4 bg-gray-300 rounded"></div>
                                            <div className="w-48 h-4 bg-gray-200 rounded"></div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Payment Information Card Skeleton */}
                    <div className="bg-white rounded-lg shadow-sm animate-pulse">
                        <div className="border-b border-gray-100 p-6">
                            <div className="flex items-center space-x-3">
                                <div className="w-9 h-9 bg-gray-300 rounded-lg"></div>
                                <div className="w-36 h-5 bg-gray-300 rounded"></div>
                            </div>
                        </div>

                        <div className="p-6">
                            {/* Payment status skeleton */}
                            <div className="mb-6">
                                <div className="w-28 h-4 bg-gray-300 rounded mb-3"></div>
                                <div className="flex items-center justify-between">
                                    <div className="flex items-center space-x-3">
                                        <div className="w-5 h-5 bg-gray-300 rounded-full"></div>
                                        <div className="w-20 h-6 bg-gray-300 rounded-full"></div>
                                    </div>
                                    <div className="w-24 h-8 bg-gray-300 rounded"></div>
                                </div>
                            </div>

                            {/* Payment details skeleton */}
                            <div className="mb-6">
                                <div className="w-28 h-4 bg-gray-300 rounded mb-4"></div>
                                <div className="space-y-4">
                                    {[1, 2, 3, 4].map((item) => (
                                        <div key={item} className="flex justify-between py-2">
                                            <div className="w-24 h-3 bg-gray-200 rounded"></div>
                                            <div className="w-32 h-3 bg-gray-200 rounded"></div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Summary skeleton */}
                            <div className="p-4 bg-gray-50 rounded-lg">
                                <div className="w-36 h-4 bg-gray-300 rounded mb-3"></div>
                                <div className="space-y-2">
                                    {[1, 2].map((item) => (
                                        <div key={item} className="flex justify-between">
                                            <div className="w-16 h-3 bg-gray-200 rounded"></div>
                                            <div className="w-24 h-3 bg-gray-200 rounded"></div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}