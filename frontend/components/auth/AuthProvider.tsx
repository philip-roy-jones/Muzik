'use client';

import {AuthContext} from "@/contexts/AuthContext";
import {useEffect, useRef, useState} from "react";
import {useBroadcastChannel} from "@/hooks/useBroadcastChannel";
import {renewTokens} from "@/utils/authService";
import Header from "@/components/shared/Header";

export default function AuthProvider({children,}: Readonly<{ children: React.ReactNode; }>) {
  const isInitialRender = useRef(true);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isBroadcastUpdate, setIsBroadcastUpdate] = useState(false); // Track if update came from broadcast


  // Custom BroadcastChannel message handler with proper typing
  const handleBroadcastMessage = (event: MessageEvent) => {
    const {type, token} = event.data as { type: string; token?: string };
    console.log(`Received message: ${type} with token: ${token}`);
    if (type === "update_token" && token) {
      setAccessToken(token);
      setIsBroadcastUpdate(true);
    } else if (type === "logout") {
      setAccessToken(null);
      setIsBroadcastUpdate(true);
    }
  };

  const channel = useBroadcastChannel("auth_channel", handleBroadcastMessage);

  // Only run on first render, fetches access token
  useEffect(() => {
    if (!channel) {
      console.log("Channel not set");
      return
    }

    // Fetches the token from the server if it is not set
    const fetchTokens = async () => {
      const accessToken = await renewTokens();
      if (accessToken) {
        setAccessToken(accessToken);
      } else {
        setAccessToken(null);
      }
    }

    fetchTokens();

  }, [])

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
      console.log("User logged in, broadcasting access token");
      if (channel) {
        channel.postMessage({type: "update_token", token: accessToken});
      }
    } else {
      console.log("User logged out, broadcasting logout");
      if (channel) {
        channel.postMessage({type: "logout"});
      }
    }
  }, [accessToken]);

  // TODO: Block all routes until accessToken is set (except login and register)
  //    Block login and register if accessToken is set
  return (
    <AuthContext.Provider value={{accessToken, setAccessToken}}>
      <Header/>
      {children}
    </AuthContext.Provider>
  );
}