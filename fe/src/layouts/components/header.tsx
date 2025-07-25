'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { COLORS, NAVIGATION } from '@/core/config/constants';

export default function Header() {
    const pathname = usePathname();

    const navItems = NAVIGATION;

    return (
        <header className="text-white shadow-lg" style={{backgroundColor: COLORS.DEEP_CHARCOAL}}>
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center h-16">
                    {/* Logo */}
                    <Link href="/" className="flex items-center space-x-2">
                        <div className="text-2xl font-bold" style={{color: COLORS.PRIMARY_GOLD}}>
                            Order Portal
                        </div>
                    </Link>

                    {/* Navigation */}
                    <nav className="hidden md:flex space-x-8">
                        {navItems.map((item) => (
                            <Link
                                key={item.href}
                                href={item.href}
                                className="px-3 py-2 text-sm font-medium transition-colors duration-200"
                                style={{
                                    color: pathname === item.href ? COLORS.PRIMARY_GOLD : '#d1d5db',
                                    borderBottom: pathname === item.href ? `2px solid ${COLORS.PRIMARY_GOLD}` : 'none'
                                }}
                                onMouseEnter={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}
                                onMouseLeave={(e) => e.currentTarget.style.color = pathname === item.href ? COLORS.PRIMARY_GOLD : '#d1d5db'}
                            >
                                {item.label}
                            </Link>
                        ))}
                    </nav>

                    {/* Mobile menu button */}
                    <div className="md:hidden">
                        <button
                            type="button"
                            className="text-gray-300 focus:outline-none"
                            style={{color: '#d1d5db'}}
                            onMouseEnter={(e) => e.currentTarget.style.color = COLORS.PRIMARY_GOLD}
                            onMouseLeave={(e) => e.currentTarget.style.color = '#d1d5db'}
                        >
                            <svg
                                className="h-6 w-6"
                                fill="none"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth="2"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                            >
                                <path d="M4 6h16M4 12h16M4 18h16"></path>
                            </svg>
                        </button>
                    </div>
                </div>
            </div>

            {/* Golden accent line */}
            <div className="h-0.5" style={{backgroundColor: COLORS.PRIMARY_GOLD}}></div>
        </header>
    );
}