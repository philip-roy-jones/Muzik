'use client'

import ProfileMenu from "@/components/shared/ProfileMenu";
import Link from "next/link";
import { useContext } from "react";
import { AuthContext } from "@/contexts/AuthContext";

export default function ProfileButton () {
  const { accessToken } = useContext(AuthContext);

  return (
    <>
      {accessToken ? (<ProfileMenu />) : (
        <Link href="/login" className="text-blue-600 underline">
          Login
        </Link>
      )}
    </>
  );
}

