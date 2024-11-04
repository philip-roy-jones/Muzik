'use client';

import {AuthContext} from "@/contexts/AuthContext";
import {useEffect, useState} from "react";
import {renewTokens} from "@/utils/authService";
import Header from "@/components/shared/Header";

export default function AuthProvider({children,}: Readonly<{ children: React.ReactNode; }>) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const channel = new BroadcastChannel('auth_channel');

  useEffect(() => {
    // Listener for receiving tokens from other tabs
    channel.onmessage = (event) => {
      const { type, token } = event.data;
      if (type === 'update_token' && token) {
        setAccessToken(token); // Set the token received from other tabs
      } else if (type === 'logout') {
        setAccessToken(null); // Clear the token on logout
      }
    };

    // Fetches the token from the server if it is not set
    const fetchOrBroadcastTokens = async () => {
      if (!accessToken) {
        const newAccessToken = await renewTokens();
        if (newAccessToken) {
          setAccessToken(newAccessToken);
          channel.postMessage({type: 'update_token', token: newAccessToken});
        } else {
          console.log("Invalid refresh token")
        }
      }
    };

    fetchOrBroadcastTokens();

    // Sync access token whenever it changes
    const broadcastAccessToken = () => {
      if (accessToken) {
        channel.postMessage({type: 'update_token', token: accessToken});
      } else {
        // If accessToken is null, broadcast logout
        channel.postMessage({ type: 'logout' });
      }
    };

    broadcastAccessToken(); // Call this whenever the access token changes

    // Clean up Broadcast Channel on unmount
    return () => {
      channel.close();
    };
  }, [accessToken]);

  // TODO: Block all routes until accessToken is set (except login and register)
  //    Block login and register if accessToken is set
  return (
    <AuthContext.Provider value={{accessToken, setAccessToken}}>
      <Header />
      {children}
    </AuthContext.Provider>
  );
}