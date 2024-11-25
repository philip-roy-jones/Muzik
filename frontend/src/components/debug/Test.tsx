'use client';

import {useAuth} from "@/src/components/auth/AuthProvider";

const Test = () => {
  const {expiration, currentUser} = useAuth();

  return (
    <>
      <p>User signed in? {expiration ? "Yes" : "No"}</p>
      <p>{"Expiration: " + expiration}</p>
      <p>{"Current User: " + JSON.stringify(currentUser)}</p>
    </>
  );
};

export default Test;