import {useBroadcastChannel} from "@/hooks/useBroadcastChannel";

export function useAuthBroadcastHandlers(
  tabUUID: string,
  isLoggedIn: boolean,
  expiryDate: Date | null,
  setIsLoggedIn: (isLoggedIn: boolean) => void,
  setExpiryDate: (date: Date | null) => void,
) {

  // Custom BroadcastChannel message handler with proper typing
  const handlePublicBroadcast = (event: MessageEvent) => {
    const {type, loginStatus, tabUUID: requestingTabUUID, expDate} = event.data as {
      type: string;
      loginStatus?: boolean;
      tabUUID?: string;
      expDate?: Date
    };
    console.log(`Received message: ${type}`);
    if (type === "login" && loginStatus) {
      setIsLoggedIn(true);
      if (expDate) {
        setExpiryDate(expDate);
      }
    } else if (type === "logout") {
      setExpiryDate(null);
      setIsLoggedIn(false);
    } else if (type === "request_token" && isLoggedIn) {
      const tempChannel = new BroadcastChannel(`private_channel_${requestingTabUUID}`);
      tempChannel.postMessage({type: "login", loginStatus: true, expDate: expiryDate});
      tempChannel.close();
    }
  };

  const handlePrivateBroadcast = (event: MessageEvent) => {
    const {type, loginStatus, expDate} = event.data as { type: string; loginStatus: boolean; expDate: Date };
    // console.log(`Received private message: ${type} with token: ${token}`);
    if (type === "login") {
      // If the new expiry date is longer than the current one, or if there is no expiry date, update it
      if ((expiryDate && expiryDate < expDate) || !expiryDate) {
        setExpiryDate(expDate);
      }
      setIsLoggedIn(loginStatus);
    }
  }

  const publicChannel = useBroadcastChannel("public_channel", handlePublicBroadcast);

  // Private channel only for receiving, so it has no usages
  const privateChannel = useBroadcastChannel(`private_channel_${tabUUID}`, handlePrivateBroadcast);

  return { publicChannel };
}