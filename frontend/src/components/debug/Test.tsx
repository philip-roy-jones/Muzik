'use client';

import {useAuth} from "@/src/components/auth/AuthProvider";

const Test = () => {
  const {expirationState, currentUserState, tokenExpirationTimerRef} = useAuth();

  return (
    <>
      <p>User signed in? {expirationState ? "Yes" : "No"}</p>
      <p>{"Expiration: " + expirationState}</p>
      <p>{"Current User: " + JSON.stringify(currentUserState)}</p>
      <p>Token Timer ID (ref, will not re-render): {tokenExpirationTimerRef?.current?.toString() ?? "null"}</p>
    </>
  );
};

export default Test;