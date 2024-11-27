'use client';
import {useAuth} from "@/src/components/auth/AuthProvider";
import {useRouter} from "next/navigation";
import {useLayoutEffect} from "react";

export default function Home() {
  const {expirationState} = useAuth();
  const router = useRouter();

  useLayoutEffect(() => {
    if (!expirationState || expirationState < new Date()) {   // No expiration or expiration is in the past
      router.push('/login');
    }
  }, [expirationState, router]);

  return (
    <>
      {expirationState ? (
        <div className="">
          <h1>Welcome to the Home Page</h1>
        </div>
      ) : null}
    </>
  );
}
