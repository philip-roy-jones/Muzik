import {useState} from "react";
import {checkTokens} from "@/utils/authService";

export function useToken() {
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);
  const [expiryDate, setExpiryDate] = useState<Date | null>(null);

  const initTokens = async () => {
    const checkingFlagKey = "isCheckingTokens";
    const isChecking = localStorage.getItem(checkingFlagKey);

    // Prevent multiple fetches, if another tab is already fetching, wait for the broadcast
    if (isChecking && isChecking === "true") {
      return;
    }

    localStorage.setItem(checkingFlagKey, "true");

    try {
      const responseObject = await checkTokens();
      if (responseObject) {
        const {expSeconds, loginStatus} = responseObject;
        setExpiryDate(new Date(Date.now() + expSeconds * 1000));
        setIsLoggedIn(loginStatus);
      } else {
        setIsLoggedIn(false);
      }
    } finally {
      localStorage.setItem(checkingFlagKey, "false");
    }
  };

  return { isLoggedIn, setIsLoggedIn, expiryDate, setExpiryDate, initTokens };
}