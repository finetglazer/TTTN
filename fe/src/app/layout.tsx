import type { Metadata } from "next";
import "@/core/assets/css/globals.css";

export const metadata: Metadata = {
    title: "Order Management Portal",
    description: "Professional order management system",
};

export default function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    return (
        <html lang="en">
        <body className="antialiased">
            {children}
        </body>
        </html>
    );
}