import type { Metadata } from "next";
// import { QueryProvider } from "@/core/providers/query-provider";
import AppLayout from "@/layouts/app-layout";
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
        {/*<QueryProvider>*/}
            <AppLayout>
                {children}
            </AppLayout>
        {/*</QueryProvider>*/}
        </body>
        </html>
    );
}