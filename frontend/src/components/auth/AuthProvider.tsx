'use client';

import {AuthContext} from "@/src/contexts/AuthContext";
import {useEffect, useRef} from "react";
import Header from "@/src/components/shared/Header";
import {v4 as uuidv4} from 'uuid';
import {useToken} from "@/src/hooks/useToken";
import {useAuthBroadcastHandlers} from "@/src/hooks/useAuthBroadcastHandlers";
import { useAuthRedirect } from '@/src/hooks/useAuthRedirect';
import { useRouter, usePathname } from 'next/navigation';

export default function AuthProvider({children,}: Readonly<{ children: React.ReactNode; }>) {
  const {username, usernameRef, setUsername, roles, setRoles, initTokens} = useToken();
  const router = useRouter();
  const pathname = usePathname();
  const isInitialRender = useRef(true);
  const tabUUID = uuidv4();

  const {publicChannel} = useAuthBroadcastHandlers(
    tabUUID,
    username,
    setUsername,
    roles,
    setRoles
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
      if (!usernameRef.current) {
        initTokens();
      }
    }, 100);

  }, []);

  // Login/Logout Broadcast Hook
  useEffect(() => {
    if (isInitialRender.current) {
      isInitialRender.current = false; // Skip on first render
      return;
    }

    if (username) {
      if (publicChannel) publicChannel.postMessage({type: "login", username: username, roles: roles});
    } else {
      if (publicChannel) publicChannel.postMessage({type: "logout"});
    }
  }, [username]);

  useAuthRedirect(username, pathname, router);

  return (
    <AuthContext.Provider value={{username, usernameRef, setUsername, roles, setRoles }}>
      <Header/>
      {children}
    </AuthContext.Provider>
  );
}