'use client'

import ProfileMenu from "@/components/shared/ProfileMenu";
import Link from "next/link";
import { useContext } from "react";
import { AuthContext } from "@/contexts/AuthContext";

export default function ProfileButton () {
  const authContext = useContext(AuthContext);
  const accessToken = authContext ? authContext.accessToken : null;

  // TODO: Broadcast channel to sync access token between tabs
  //  First - Checks if there are any tabs with a valid access token
  //  Second - If there are no tabs with a valid access token, then it will renew the access token
  return (
    <>
      <p>{accessToken}</p>
      {accessToken ? (<ProfileMenu />) : (
        <Link href="/login" className="text-blue-600 underline">
          Login
        </Link>
      )}
    </>
  );
}

