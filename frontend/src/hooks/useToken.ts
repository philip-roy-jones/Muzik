import {useState} from "react";
import {check} from "@/src/utils/authService";
import useStateWithRef from "@/src/hooks/useStateWithRef";

export function useToken() {
  const [username, usernameRef, setUsername] = useStateWithRef("");
  const [roles, setRoles] = useState<string[]>([]);

  const initTokens = async () => {
    const checkingFlagKey = "isCheckingTokens";
    const isChecking = localStorage.getItem(checkingFlagKey);

    // Prevent multiple fetches, if another tab is already fetching, wait for the broadcast
    if (isChecking && isChecking === "true") {
      return;
    }

    localStorage.setItem(checkingFlagKey, "true");

    try {
      const responseObject = await check();
      if (responseObject) {
        const {username, roles} = responseObject;
        setRoles(roles);
        setUsername(username);
      } else {
        setUsername("");
      }
    } finally {
      localStorage.setItem(checkingFlagKey, "false");
    }
  };

  return { username, usernameRef, setUsername, roles, setRoles, initTokens };
}