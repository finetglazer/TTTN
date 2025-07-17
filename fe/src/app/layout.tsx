import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
    title: "Order Management Portal",
    description: "Professional order management system with luxury design",
};

export default function RootLayout({
                                       children,
                                   }: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html lang="en">
        <body className="antialiased">
        {children}
        </body>
        </html>
    );
}