'use client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {queryClient} from "@/core/config/query-client";

export function QueryProvider({ children }: { children: React.ReactNode }) {
    return (
        <QueryClientProvider client={queryClient}>
            {children}
        </QueryClientProvider>
    );
}