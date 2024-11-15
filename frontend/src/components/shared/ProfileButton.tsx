'use client'

import ProfileMenu from "@/src/components/shared/ProfileMenu";
import Link from "next/link";
import { useContext } from "react";
import { AuthContext } from "@/src/contexts/AuthContext";

export default function ProfileButton () {
  const authContext = useContext(AuthContext);
  const isLoggedIn = authContext?.username ?? false;

  return (
    <>
      {isLoggedIn ? (<ProfileMenu />) : (
        <Link href="/login" className="text-blue-600 underline">
          Login
        </Link>
      )}
    </>
  );
}

