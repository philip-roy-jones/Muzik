'use client'

import ProfileMenu from "@/components/shared/ProfileMenu";
import Link from "next/link";
import { useContext } from "react";
import { AuthContext } from "@/contexts/AuthContext";

export default function ProfileButton () {
  const authContext = useContext(AuthContext);
  const accessToken = authContext ? authContext.accessToken : null;

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

