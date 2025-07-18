'use client';

// import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

export default function WelcomePage() {
    const [isLoaded, setIsLoaded] = useState(false);
    const [isTransitioning, setIsTransitioning] = useState(false);
    const router = useRouter();

    useEffect(() => {
        setIsLoaded(true);
    }, []);

    const handleTransition = () => {
        setIsTransitioning(true);
        // Wait for animation to complete before navigation
        setTimeout(() => {
            router.push('/dashboard');
        }, 800);
    };

    return (
        <>
            {/* Transition Overlay - Updated to match darker theme */}
            <div
                className={`fixed inset-0 z-50 bg-gradient-to-t from-black via-gray-900 to-black transition-transform duration-800 ease-in-out ${
                    isTransitioning ? 'transform translate-y-0' : 'transform translate-y-full'
                }`}
            />

            {/* Main Welcome Page */}
            <div className={`min-h-screen relative overflow-hidden transition-transform duration-800 ease-in-out ${
                isTransitioning ? 'transform -translate-y-full' : 'transform translate-y-0'
            }`}>
                {/* Much Darker Background with better gradient and pattern */}
                <div
                    className="absolute inset-0 bg-gradient-to-br from-black via-gray-900 to-black"
                    style={{
                        backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23f6d55c' fill-opacity='0.03'%3E%3Ccircle cx='30' cy='30' r='2'/%3E%3Ccircle cx='15' cy='15' r='1'/%3E%3Ccircle cx='45' cy='45' r='1'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
                    }}
                />

                {/* Additional dark overlay for better text contrast */}
                <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-gray-900/50 to-black/30" />

                {/* Header */}
                <header className="relative z-10 p-6">
                    <div className="max-w-7xl mx-auto">
                        <div className="text-2xl font-bold text-[#f6d55c] tracking-wide">
                            Order Portal
                        </div>
                    </div>
                </header>

                {/* Main Content */}
                <main className="relative z-10 flex items-center justify-center min-h-[80vh] px-6">
                    <div className={`text-center max-w-3xl mx-auto transition-all duration-1000 ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'}`}>
                        {/* Logo/Title - Enhanced with better spacing and size */}
                        <h1 className="text-6xl md:text-8xl font-bold text-[#f6d55c] mb-8 tracking-tight leading-tight">
                            Order Management
                        </h1>

                        {/* Welcome Message - Improved typography */}
                        <h2 className="text-3xl md:text-5xl font-semibold text-white mb-6 leading-tight">
                            Welcome to Your Portal
                        </h2>

                        {/* Subtitle - Better contrast and readability on dark background */}
                        <p className="text-xl md:text-2xl text-gray-200 mb-12 leading-relaxed max-w-2xl mx-auto">
                            Streamline your order operations with our sophisticated management system.
                            Track, manage, and monitor all your orders with luxury-grade precision.
                        </p>

                        {/* Enhanced Enter Button */}
                        <button
                            onClick={handleTransition}
                            className="inline-block bg-gradient-to-r from-[#f6d55c] to-[#e6c53f] hover:from-[#e6c53f] hover:to-[#d4b739] text-[#1a1a1a] text-xl font-semibold px-12 py-4 rounded-lg transition-all duration-300 transform hover:scale-105 hover:shadow-2xl hover:shadow-[#f6d55c]/25 active:scale-95"
                        >
                            Enter Portal
                        </button>

                        {/* Subtle hint with improved animation and better contrast */}
                        {/*<p className="text-sm text-gray-300 mt-8 opacity-75 animate-pulse">*/}
                        {/*    Click anywhere to continue your journey*/}
                        {/*</p>*/}
                    </div>
                </main>

                {/* Footer */}
                <footer className="relative z-10 p-6 text-center">
                    <div className="max-w-7xl mx-auto">
                        <p className="text-gray-300 text-sm">
                            © 2025 Order Management Portal. Crafted with precision.
                        </p>
                    </div>
                </footer>

                {/* Enhanced Click anywhere functionality */}
                <div
                    className="absolute inset-0 z-5 cursor-pointer"
                    onClick={handleTransition}
                />
            </div>
        </>
    );
}