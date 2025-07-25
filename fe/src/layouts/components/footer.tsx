'use client';

import Link from 'next/link';
import { COLORS, CONTACT, ROUTES } from '@/core/config/constants';

export default function Footer() {
    return (
        <footer className="text-white border-t border-gray-800" style={{backgroundColor: COLORS.DEEP_CHARCOAL}}>
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
                    {/* Company Info */}
                    <div className="space-y-4">
                        <h3 className="text-2xl font-bold tracking-wide" style={{color: COLORS.PRIMARY_GOLD}}>
                            OrderPortal
                        </h3>
                        <p className="text-gray-400 text-sm leading-relaxed">
                            Professional order management system designed for efficiency and precision in your business operations.
                        </p>
                    </div>

                    {/* Quick Links */}
                    <div className="space-y-4">
                        <h4 className="text-lg font-semibold text-white">Quick Links</h4>
                        <ul className="space-y-2">
                            <li>
                                {/*<Link*/}
                                {/*    href="/dashboard"*/}
                                {/*    className="text-gray-400 transition-colors duration-200"*/}
                                {/*    onMouseEnter={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}*/}
                                {/*    onMouseLeave={(e) => e.currentTarget.style.color = '#9ca3af'}*/}
                                {/*>*/}
                                {/*    Dashboard*/}
                                {/*</Link>*/}
                            </li>
                            <li>
                                <Link
                                    href="/create-order"
                                    className="text-gray-400 transition-colors duration-200"
                                    onMouseEnter={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}
                                    onMouseLeave={(e) => e.currentTarget.style.color = '#9ca3af'}
                                >
                                    Create Order
                                </Link>
                            </li>
                            <li>
                                <Link
                                    href="/orders"
                                    className="text-gray-400 transition-colors duration-200"
                                    onMouseEnter={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}
                                    onMouseLeave={(e) => e.currentTarget.style.color = '#9ca3af'}
                                >
                                    All Orders
                                </Link>
                            </li>
                        </ul>
                    </div>

                    {/* Support */}
                    <div className="space-y-4">
                        <h4 className="text-lg font-semibold text-white">Support</h4>
                        <ul className="space-y-2">
                            <li>
                                <a href="#" className="text-gray-400 hover:text-[#f6d55c] transition-colors duration-200">
                                    Help Center
                                </a>
                            </li>
                            <li>
                                <a href="#" className="text-gray-400 hover:text-[#f6d55c] transition-colors duration-200">
                                    Contact Us
                                </a>
                            </li>
                            <li>
                                <a href="#" className="text-gray-400 hover:text-[#f6d55c] transition-colors duration-200">
                                    Documentation
                                </a>
                            </li>
                        </ul>
                    </div>

                    {/* Contact Info */}
                    <div className="space-y-4">
                        <h4 className="text-lg font-semibold text-white">Contact</h4>
                        <div className="space-y-2 text-gray-400 text-sm">
                            <p>{CONTACT.SUPPORT_EMAIL}</p>
                            <p>{CONTACT.SUPPORT_PHONE}</p>
                            <p>{CONTACT.BUSINESS_HOURS}</p>
                        </div>
                    </div>
                </div>

                {/* Bottom Border */}
                <div className="border-t border-gray-800 mt-12 pt-8">
                    <div className="flex flex-col md:flex-row justify-between items-center">
                        <p className="text-gray-400 text-sm">
                            &copy; {new Date().getFullYear()} OrderPortal. All rights reserved.
                        </p>
                        <div className="flex items-center space-x-6 mt-4 md:mt-0">
                            <a
                                href="#"
                                className="text-gray-400 text-sm transition-colors duration-200"
                                onMouseEnter={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}
                                onMouseLeave={(e) => e.currentTarget.style.color = '#9ca3af'}
                            >
                                Privacy Policy
                            </a>
                            <a
                                href="#"
                                className="text-gray-400 text-sm transition-colors duration-200"
                                onMouseEnter={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}
                                onMouseLeave={(e) => e.currentTarget.style.color = '#9ca3af'}
                            >
                                Terms of Service
                            </a>
                            <a
                                href="#"
                                className="text-gray-400 text-sm transition-colors duration-200"
                                onMouseEnter={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}
                                onMouseLeave={(e) => e.currentTarget.style.color = '#9ca3af'}
                            >
                                Cookie Policy
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </footer>
    );
}