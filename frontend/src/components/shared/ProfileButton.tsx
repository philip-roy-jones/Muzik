'use client'

import ProfileMenu from "@/src/components/shared/ProfileMenu";
import Link from "next/link";
import {useAuth} from "@/src/components/auth/AuthProvider";

export default function ProfileButton () {
  const { accessToken } = useAuth();

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

