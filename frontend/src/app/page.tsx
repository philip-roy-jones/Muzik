'use client';
import {useAuth} from "@/src/components/auth/AuthProvider";
import {useRouter} from "next/navigation";
import {useLayoutEffect} from "react";

export default function Home() {
  const {accessToken, currentUser, handleLogin, handleLogout} = useAuth();
  const router = useRouter();

  useLayoutEffect(() => {
    if (!accessToken) {
      router.push('/login');
    }
  }, [accessToken, router]);

  return (
    <>
      {accessToken ? (
        <div className="">
          <h1>Welcome to the Home Page</h1>
        </div>
      ) : null}
    </>
  );
}
