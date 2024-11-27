import {useBroadcastChannel} from "@/src/hooks/useBroadcastChannel";
import {User} from "@/src/types/user";

export function useAuthBroadcastHandlers(
  tabUuid: string,
  expirationState: Date | null | undefined,
  setExpirationState: React.Dispatch<React.SetStateAction<Date | null | undefined>>,
  currentUserState: User | null | undefined,
  setCurrentUserState: React.Dispatch<React.SetStateAction<User | null | undefined>>,
  authSetByBroadcastRef: React.MutableRefObject<boolean | undefined>,
  setTokenExpirationTimer:(expiration: Date) => void
) {

  // Custom BroadcastChannel message handler with proper typing
  const handlePublicBroadcast = (event: MessageEvent) => {
    const {type, senderExpiration, senderTabUUID, senderCurrentUser} = event.data as {
      type: string;
      senderTabUUID?: string;
      senderExpiration?: Date;
      senderCurrentUser?: User;
    };

    if (type === "UPDATE") {      // Login or logout
      if (senderExpiration && senderCurrentUser) {
        setExpirationState(senderExpiration);
        setCurrentUserState(senderCurrentUser);
        setTokenExpirationTimer(senderExpiration);
      } else {
        setExpirationState(null);
        setCurrentUserState(null);
        setTokenExpirationTimer(new Date(new Date().getTime() - 10000));    // Set expiration to 10 seconds ago to clear timeout
      }
    } else if (type === "AUTH_REQUEST" && expirationState instanceof Date && currentUserState) {
      console.log(`Sending private message to private_auth_${senderTabUUID}`);

      const tempChannel = new BroadcastChannel(`private_auth_${senderTabUUID}`);   // Temporary channel to send private message
      tempChannel.postMessage({
        type: "AUTH_RESPONSE",
        senderExpiration: expirationState,
        senderCurrentUser: currentUserState
      });
      tempChannel.close();
    }
  };

  const handlePrivateBroadcast = (event: MessageEvent) => {
    const {type, senderExpiration, senderCurrentUser} = event.data as {
      type: string;
      senderExpiration: Date;
      senderCurrentUser: User
    };

    if (type === "AUTH_RESPONSE") {
      console.log("Received AUTH_RESPONSE");
      setExpirationState(senderExpiration);
      setCurrentUserState(senderCurrentUser);

      if (authSetByBroadcastRef && authSetByBroadcastRef.current !== undefined) {
        authSetByBroadcastRef.current = true;
      }

      setTokenExpirationTimer(senderExpiration);
    }
  }

  const publicAuth = useBroadcastChannel("public_auth", handlePublicBroadcast);

  // Private channel only for receiving, so it has no usages
  useBroadcastChannel(`private_auth_${tabUuid}`, handlePrivateBroadcast);

  return {publicAuth};
}