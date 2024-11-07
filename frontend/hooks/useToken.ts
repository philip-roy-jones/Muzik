import {useRef, useState} from "react";
import {renewTokens} from "@/utils/authService";

export function useToken() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const accessTokenRef = useRef<string | null>(null); // Ref to store the latest token without causing re-renders
  const [expiryDate, setExpiryDate] = useState<Date | null>(null);

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
  };

  return { accessToken, setAccessToken, expiryDate, setExpiryDate, fetchTokens, accessTokenRef };
}