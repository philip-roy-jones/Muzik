import {useBroadcastChannel} from "@/src/hooks/useBroadcastChannel";

export function useAuthBroadcastHandlers(
  tabUUID: string,
  username: string,
  setUsername: (status: string) => void,
  roles: string[],
  setRoles: (roles: string[]) => void,
) {

  // Custom BroadcastChannel message handler with proper typing
  const handlePublicBroadcast = (event: MessageEvent) => {
    const {type, username, tabUUID: requestingTabUUID, roles} = event.data as {
      type: string;
      username?: string;
      tabUUID?: string;
      roles?: string[];
    };
    console.log(`Received message: ${type}`);
    if (type === "login" && username) {
      setUsername(username);
      if (roles) {
        setRoles(roles);
      }
    } else if (type === "logout") {
      setRoles([])
      setUsername("");
    } else if (type === "request_token" && username) {
      const tempChannel = new BroadcastChannel(`private_channel_${requestingTabUUID}`);
      tempChannel.postMessage({type: "login", username: username, roles: roles});
      tempChannel.close();
    }
  };

  const handlePrivateBroadcast = (event: MessageEvent) => {
    const {type, username, roles} = event.data as { type: string; username: string; roles: string[] };
    console.log(`Received private message: ${type} with username: ${username}`);
    if (type === "login") {
      setRoles(roles);
      setUsername(username);
    }
  }

  const publicChannel = useBroadcastChannel("public_channel", handlePublicBroadcast);

  // Private channel only for receiving, so it has no usages
  useBroadcastChannel(`private_channel_${tabUUID}`, handlePrivateBroadcast);

  return { publicChannel };
}