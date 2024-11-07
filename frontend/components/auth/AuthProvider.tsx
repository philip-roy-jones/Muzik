'use client';

import {AuthContext} from "@/contexts/AuthContext";
import {useEffect, useRef, useState} from "react";
import {useBroadcastChannel} from "@/hooks/useBroadcastChannel";
import {renewTokens} from "@/utils/authService";
import Header from "@/components/shared/Header";
import {v4 as uuidv4} from 'uuid';

export default function AuthProvider({children,}: Readonly<{ children: React.ReactNode; }>) {
  const isInitialRender = useRef(true);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const accessTokenRef = useRef<string | null>(null); // Ref to store the latest token without causing re-renders
  const [expiryDate, setExpiryDate] = useState<Date | null>(null);
  const [isBroadcastUpdate, setIsBroadcastUpdate] = useState(false); // Track if update came from broadcast


  // Custom BroadcastChannel message handler with proper typing
  const handlePublicBroadcast = (event: MessageEvent) => {
    const {type, token, tabUUID: requestingTabUUID, expDate} = event.data as {
      type: string;
      token?: string;
      tabUUID?: string;
      expDate?: Date
    };
    console.log(`Received message: ${type}`);
    if (type === "login" && token) {
      if (expDate) {
        setExpiryDate(expDate);
      }
      setAccessToken(token);
      setIsBroadcastUpdate(true);
    } else if (type === "logout") {
      setExpiryDate(null);
      setAccessToken(null);
      setIsBroadcastUpdate(true);
    } else if (type === "request_token" && accessToken) {
      const tempChannel = new BroadcastChannel(`private_channel_${requestingTabUUID}`);
      tempChannel.postMessage({type: "login", token: accessToken, expDate: expiryDate});
      tempChannel.close();
    }
  };

  const handlePrivateBroadcast = (event: MessageEvent) => {
    const {type, token, expDate} = event.data as { type: string; token: string; expDate: Date };
    // console.log(`Received private message: ${type} with token: ${token}`);
    if (type === "login" && token) {
      // If the new expiry date is longer than the current one, or if there is no expiry date, update it
      if ((expiryDate && expiryDate < expDate) || !expiryDate) {
        setExpiryDate(expDate);
      }
      setAccessToken(token);
      accessTokenRef.current = token;
      console.log(`Access Token has been set`);
      setIsBroadcastUpdate(true);
    }
  }

  const tabUUID = uuidv4();

  const publicChannel = useBroadcastChannel("public_channel", handlePublicBroadcast);
  // Private channel only for receiving, no usages
  const privateChannel = useBroadcastChannel(`private_channel_${tabUUID}`, handlePrivateBroadcast);

  // Only run on first render, fetches access token
  // Access token is stored in memory, so by default it will be null every time the page is refreshed/newly opened
  useEffect(() => {
    const requestToken = async () => {
      console.log("Running")
      if (publicChannel) {
        publicChannel.postMessage({type: "request_token", tabUUID: tabUUID});
      }
      console.log(`UseEffect Finished`)
    }
    requestToken();
  }, [])

  useEffect(() => {
    console.log("Second running")
    const fetchTokens = async () => {
      const fetchingFlagKey = "isFetchingToken";
      const isFetching = localStorage.getItem(fetchingFlagKey);

      // Prevent multiple fetches, if another tab is already fetching, wait for the broadcast
      if (isFetching && isFetching === "true") {
        return;
      }

      localStorage.setItem(fetchingFlagKey, "true");

      try {
        const responseObject = await renewTokens();
        if (responseObject) {
          const {accToken, expSeconds} = responseObject;
          setExpiryDate(new Date(Date.now() + expSeconds * 1000));
          setAccessToken(accToken);
          accessTokenRef.current = accToken;
        } else {
          setAccessToken(null);
          accessTokenRef.current = null;
        }
      } finally {
        localStorage.setItem(fetchingFlagKey, "false");
      }
    }

    // If no response is received within 100 ms, client hardware dependent
    setTimeout(() => {
      console.log(`Access Token: ${accessToken}`)
      if (!accessTokenRef.current) {
        // TODO: This will try even when user is not logged in, maybe have something in localstorage indicating
        //  if user is logged in?
        fetchTokens();
      }
    }, 100);

  }, []);

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
      // console.log("User logged in, broadcasting access token");
      if (publicChannel) {
        publicChannel.postMessage({type: "login", token: accessToken, expDate: expiryDate});
      }
    } else {
      // console.log("User logged out, broadcasting logout");
      if (publicChannel) {
        publicChannel.postMessage({type: "logout"});
      }
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