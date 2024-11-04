'use client';

import {AuthContext} from "@/contexts/AuthContext";
import {useEffect, useRef, useState} from "react";
import {renewTokens} from "@/utils/authService";
import Header from "@/components/shared/Header";

export default function AuthProvider({children,}: Readonly<{ children: React.ReactNode; }>) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const channelRef = useRef<BroadcastChannel | null>(null);

  useEffect(() => {
    // Listener for receiving tokens from other tabs
    const channel = new BroadcastChannel('auth_channel');
    channelRef.current = channel;

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

        // Check if another tab has already set the token
        let tokenFromChannel: string | null = null;
        const tokenListener = (event: MessageEvent) => {
          if (event.data.type === 'update_token' && event.data.token) {
            tokenFromChannel = event.data.token;
          }
        };
        channel.addEventListener('message', tokenListener);

        // Wait a short period to see if another tab broadcasts the token
        await new Promise(resolve => setTimeout(resolve, 500));
        channel.removeEventListener('message', tokenListener);

        if (tokenFromChannel) {
          setAccessToken(tokenFromChannel);
        } else {
          const newAccessToken = await renewTokens();
          if (newAccessToken) {
            setAccessToken(newAccessToken);
            if (channelRef.current) {
              channelRef.current.postMessage({type: 'update_token', token: newAccessToken});
            }
          } else {
            console.log("Invalid refresh token")
          }
        }
      }
    };

    fetchOrBroadcastTokens();

    // Sync access token whenever it changes
    const broadcastAccessToken = () => {
      if (accessToken) {
        if (channelRef.current) {
          channelRef.current.postMessage({type: 'update_token', token: accessToken});
        }
      } else {
        // If accessToken is null, broadcast logout
        if (channelRef.current) {
          channelRef.current.postMessage({ type: 'logout' });
        }
      }
    };

    broadcastAccessToken(); // Call this whenever the access token changes

    // Clean up Broadcast Channel on unmount
    return () => {
      if (channelRef.current) {
        channelRef.current.close();
        channelRef.current = null;
      }
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