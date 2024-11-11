'use client';

import {useContext} from "react";
import {AuthContext} from "@/contexts/AuthContext";

const Test = () => {
  const authContext = useContext(AuthContext);
  const expiryDate = authContext ? authContext.expiryDate : null;
  const isLoggedIn = authContext ? authContext.isLoggedIn : false;

  return (
    <>
      <p>{"Access Token Expiry: " + (expiryDate ? new Date(expiryDate).toString() : "No expiry date")}</p>
      <p>{"isLoggedIn: " + (isLoggedIn ? "true" : "false")}</p>
    </>
  );
};

export default Test;