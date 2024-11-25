'use client'

import ProfileMenu from "@/src/components/shared/ProfileMenu";
import Link from "next/link";
import {useQueryClient} from "@tanstack/react-query";

export default function ProfileButton () {
  const queryClient = useQueryClient();

  const data = queryClient.getQueryData<{ expiration: Date | undefined }>(['auth']) || { expiration: undefined };
  const expiration = data?.expiration;

  return (
    <>
      {expiration && expiration > new Date() ? (<ProfileMenu />) : (
        <Link href="/login" className="text-blue-600 underline">
          Login
        </Link>
      )}
    </>
  );
}

