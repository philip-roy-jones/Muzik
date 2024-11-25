'use client';
import {useAuth} from "@/src/components/auth/AuthProvider";
import {useRouter} from "next/navigation";
import {useLayoutEffect} from "react";

export default function Home() {
  const {expiration} = useAuth();
  const router = useRouter();

  useLayoutEffect(() => {
    if (!expiration || expiration < new Date()) {   // No expiration or expiration is in the past
      router.push('/login');
    }
  }, [expiration, router]);

  return (
    <>
      {expiration ? (
        <div className="">
          <h1>Welcome to the Home Page</h1>
        </div>
      ) : null}
    </>
  );
}
