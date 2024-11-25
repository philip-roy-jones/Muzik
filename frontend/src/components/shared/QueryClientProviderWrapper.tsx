'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AuthProvider from '@/src/components/auth/AuthProvider';
import {ReactQueryDevtools} from "@tanstack/react-query-devtools";
import React from "react";

const queryClient = new QueryClient();

export default function QueryClientProviderWrapper({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        {children}
      </AuthProvider>
      <ReactQueryDevtools initialIsOpen={false}/>
    </QueryClientProvider>
  );
}
