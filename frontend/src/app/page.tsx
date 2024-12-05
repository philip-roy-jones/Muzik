'use client';
import {useAuth} from "@/src/components/auth/AuthProvider";
import {useRouter} from "next/navigation";
import {useLayoutEffect} from "react";
import ConnectSpotifyButton from "@/src/components/root/ConnectSpotifyButton";

export default function Home() {
  const {expirationState, currentUserState} = useAuth();
  const router = useRouter();

  useLayoutEffect(() => {
    if (!expirationState || expirationState < new Date()) {   // No expiration or expiration is in the past
      router.push('/login');
    }
  }, [expirationState, router]);

  return (
    <>
      {currentUserState?.isSpotifyConnected ? "Spotify Connected" : <ConnectSpotifyButton/>}
    </>
  );
}
