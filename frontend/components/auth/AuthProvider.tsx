'use client';

import {AuthContext} from "@/contexts/AuthContext";
import {useEffect, useRef} from "react";
import Header from "@/components/shared/Header";
import {v4 as uuidv4} from 'uuid';
import {useToken} from "@/hooks/useToken";
import {useAuthBroadcastHandlers} from "@/hooks/useAuthBroadcastHandlers";
import { useAuthRedirect } from '@/hooks/useAuthRedirect';
import { useRouter, usePathname } from 'next/navigation';

export default function AuthProvider({children,}: Readonly<{ children: React.ReactNode; }>) {
  const {isLoggedIn, isLoggedInRef, setIsLoggedIn, expiryDate, setExpiryDate, initTokens} = useToken();
  const router = useRouter();
  const pathname = usePathname();
  const isInitialRender = useRef(true);
  const tabUUID = uuidv4();

  const {publicChannel} = useAuthBroadcastHandlers(
    tabUUID,
    isLoggedIn,
    setIsLoggedIn,
    expiryDate,
    setExpiryDate
  );

  // Only run on first render, checks if other tabs have token, if not fetches access token.
  // Access token is stored in memory, so by default it will be null every time the page is refreshed/newly opened
  useEffect(() => {
    console.log("AuthProvider useEffect triggered");
    if (publicChannel) publicChannel.postMessage({type: "request_token", tabUUID: tabUUID});

    // If no response is received within 100 ms, fetch new token. This is client hardware dependent.
    setTimeout(() => {
      // Uses ref instead of state so it can track the login status through re-renders (ie. state is updated in middle of timeout, which causes re-render,
      // but the setTimeout function will still use the old state value)
      if (!isLoggedInRef.current) {
        initTokens();
      }
    }, 100);

  }, []);

  // TODO: This will still try to fetchTokens() even when user is not logged in, maybe have something in localstorage indicating
  //  if user is logged in?

  // Login/Logout Broadcast Hook
  useEffect(() => {
    if (isInitialRender.current) {
      isInitialRender.current = false; // Skip on first render
      return;
    }

    if (isLoggedIn) {
      if (publicChannel) publicChannel.postMessage({type: "login", loginStatus: isLoggedIn, expDate: expiryDate});
    } else {
      if (publicChannel) publicChannel.postMessage({type: "logout"});
    }
  }, [isLoggedIn]);

  useAuthRedirect(isLoggedIn, pathname, router);

  return (
    <AuthContext.Provider value={{isLoggedIn, isLoggedInRef, setIsLoggedIn, expiryDate, setExpiryDate}}>
      <Header/>
      {children}
    </AuthContext.Provider>
  );
}