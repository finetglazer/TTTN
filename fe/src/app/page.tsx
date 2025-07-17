'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';

export default function WelcomePage() {
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    setIsLoaded(true);
  }, []);

  return (
      <div className="min-h-screen relative overflow-hidden">
        {/* Background with gradient and pattern */}
        <div
            className="absolute inset-0 bg-gradient-to-br from-[#1a1a1a] via-[#2d3748] to-[#1a1a1a]"
            style={{
              backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23f6d55c' fill-opacity='0.05'%3E%3Ccircle cx='30' cy='30' r='4'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
            }}
        />

        {/* Header */}
        <header className="relative z-10 p-6">
          <div className="max-w-7xl mx-auto">
            <div className="text-2xl font-bold text-[#f6d55c]">
              OrderPortal
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="relative z-10 flex items-center justify-center min-h-[80vh] px-6">
          <div className={`text-center max-w-2xl mx-auto ${isLoaded ? 'animate-fade-in' : 'opacity-0'}`}>
            {/* Logo/Title */}
            <h1 className="text-6xl md:text-7xl font-bold text-[#f6d55c] mb-6 tracking-tight">
              Order Management
            </h1>

            {/* Welcome Message */}
            <h2 className="text-3xl md:text-4xl font-semibold text-white mb-4">
              Welcome to Your Portal
            </h2>

            {/* Subtitle */}
            <p className="text-xl text-[#718096] mb-12 leading-relaxed">
              Streamline your order operations with our sophisticated management system.
              Track, manage, and monitor all your orders with luxury-grade precision.
            </p>

            {/* Enter Button */}
            <Link
                href="/dashboard"
                className="inline-block btn-primary text-lg px-12 py-4 animate-pulse-glow"
            >
              Enter Portal
            </Link>

            {/* Subtle hint */}
            <p className="text-sm text-[#718096] mt-8 opacity-75">
              Click anywhere to continue your journey
            </p>
          </div>
        </main>

        {/* Footer */}
        <footer className="relative z-10 p-6 text-center">
          <div className="max-w-7xl mx-auto">
            <p className="text-[#718096] text-sm">
              © 2025 Order Management Portal. Crafted with precision.
            </p>
          </div>
        </footer>

        {/* Click anywhere functionality */}
        <div
            className="absolute inset-0 z-0 cursor-pointer"
            onClick={() => window.location.href = '/dashboard'}
        />
      </div>
  );
}