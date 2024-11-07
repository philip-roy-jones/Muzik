import {useState} from "react";
import {useBroadcastChannel} from "@/hooks/useBroadcastChannel";

export function useAuthBroadcastHandlers(
  tabUUID: string,
  accessToken: string | null,
  accessTokenRef: React.MutableRefObject<string | null>,
  expiryDate: Date | null,
  setAccessToken: (token: string | null) => void,
  setExpiryDate: (date: Date | null) => void,
) {
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

  const publicChannel = useBroadcastChannel("public_channel", handlePublicBroadcast);

  // Private channel only for receiving, so it has no usages
  const privateChannel = useBroadcastChannel(`private_channel_${tabUUID}`, handlePrivateBroadcast);

  return { isBroadcastUpdate, setIsBroadcastUpdate, publicChannel };
}