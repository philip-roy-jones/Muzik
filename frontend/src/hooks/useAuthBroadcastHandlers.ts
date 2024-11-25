import {useBroadcastChannel} from "@/src/hooks/useBroadcastChannel";
import {useQueryClient} from "@tanstack/react-query";
import {User} from "@/src/types/user";

type AuthData = {
  expiration: Date;
  currentUser: User | null;
};

export function useAuthBroadcastHandlers(
  tabUuidRef: React.MutableRefObject<string | undefined>,
) {
  const queryClient = useQueryClient();
  const authData = queryClient.getQueryData<AuthData>(["auth"]);
  const expiration = authData?.expiration;
  const currentUser = authData?.currentUser;

  // Custom BroadcastChannel message handler with proper typing
  const handlePublicBroadcast = (event: MessageEvent) => {
    const {type, senderExpiration, senderTabUUID, senderCurrentUser} = event.data as {
      type: string;
      senderTabUUID?: string;
      senderExpiration?: Date;
      senderCurrentUser?: User;
    };
    if (senderTabUUID === tabUuidRef.current) return;   // Ignore messages from self

    if (type === "UPDATE") {      // Login or logout
      console.log(senderExpiration, senderCurrentUser);
      queryClient.setQueryData(["auth"], { expiration: senderExpiration, currentUser: senderCurrentUser });
      // TODO: Clear any timers that try to refresh the token if logging out
      //  Set timers to refresh the token if logging in
    }
    else if (type === "AUTH_REQUEST" && expiration instanceof Date) {
      console.log(`Sending private message to private_auth_${senderTabUUID}`);
      console.log(`My Expiration: ${expiration}`);
      const tempChannel = new BroadcastChannel(`private_auth_${senderTabUUID}`);   // Temporary channel to send private message
      tempChannel.postMessage({type: "AUTH_RESPONSE", senderExpiration:expiration, senderCurrentUser:currentUser});
      tempChannel.close();
    }
  };

  const handlePrivateBroadcast = (event: MessageEvent) => {
    const {type, senderExpiration , senderCurrentUser} = event.data as { type: string; senderExpiration: Date; senderCurrentUser: User };

    if (type === "AUTH_RESPONSE") {
      console.log("Received AUTH_RESPONSE");
      queryClient.setQueryData(["auth"], {expiration: senderExpiration, currentUser:senderCurrentUser});
      // TODO: Handle expiration separately from react query
    }
  }

  const publicAuth = useBroadcastChannel("public_auth", handlePublicBroadcast);

  // Private channel only for receiving, so it has no usages
  useBroadcastChannel(`private_auth_${tabUuidRef.current}`, handlePrivateBroadcast);

  return {publicAuth};
}