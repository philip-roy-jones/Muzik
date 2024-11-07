'use client';

import {AuthContext} from "@/contexts/AuthContext";
import {useEffect, useRef} from "react";
import Header from "@/components/shared/Header";
import {v4 as uuidv4} from 'uuid';
import {useToken} from "@/hooks/useToken";
import {useAuthBroadcastHandlers} from "@/hooks/useAuthBroadcastHandlers";

export default function AuthProvider({children,}: Readonly<{ children: React.ReactNode; }>) {
  const {accessToken, setAccessToken, expiryDate, setExpiryDate, fetchTokens, accessTokenRef} = useToken();
  const isInitialRender = useRef(true);
  const tabUUID = uuidv4();

  const {isBroadcastUpdate, setIsBroadcastUpdate, publicChannel} = useAuthBroadcastHandlers(
    tabUUID,
    accessToken,
    accessTokenRef,
    expiryDate,
    setAccessToken,
    setExpiryDate
  );

  // Only run on first render, checks if other tabs have token, if not fetches access token
  // Access token is stored in memory, so by default it will be null every time the page is refreshed/newly opened
  useEffect(() => {
    if (publicChannel) publicChannel.postMessage({type: "request_token", tabUUID: tabUUID});

    // If no response is received within 100 ms, fetch new token. This is client hardware dependent.
    setTimeout(() => {
      if (!accessTokenRef.current) fetchTokens();
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

    if (isBroadcastUpdate) {
      // If the update was caused by a broadcast, don't execute the broadcast action
      setIsBroadcastUpdate(false); // Reset the flag
      return;
    }

    if (accessToken) {
      if (publicChannel) publicChannel.postMessage({type: "login", token: accessToken, expDate: expiryDate});
    } else {
      if (publicChannel) publicChannel.postMessage({type: "logout"});
    }
  }, [accessToken]);

  // TODO: Block all routes until accessToken is set (except login and register)
  //    Block login and register if accessToken is set
  return (
    <AuthContext.Provider value={{accessToken, setAccessToken, expiryDate, setExpiryDate}}>
      <Header/>
      {children}
    </AuthContext.Provider>
  );
}