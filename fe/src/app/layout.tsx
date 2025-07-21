import type { Metadata } from "next";
import "@/core/assets/css/globals.css";
import { APP } from "@/core/config/constants";

export const metadata: Metadata = {
    title: APP.TITLE,
    description: APP.DESCRIPTION,
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