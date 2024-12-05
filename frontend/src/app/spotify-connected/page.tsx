'use client';

import React, {useLayoutEffect, useState} from 'react';
import {verifySpotifyConnection} from "@/src/api/spotify";
import {useAuth} from "@/src/components/auth/AuthProvider";

const Page = () => {
  const {expirationState, currentUserState, setCurrentUserState} = useAuth();
  const [result, setResult] = useState<boolean | undefined>(undefined);

  useLayoutEffect(() => {
    if (!currentUserState) return;

    const verifyConnection = async () => {
        const connectionResult = await verifySpotifyConnection();
        setResult(connectionResult);

        if (currentUserState.isSpotifyConnected !== connectionResult) {
          const newCurrentUserState = {...currentUserState, isSpotifyConnected: connectionResult};
          setCurrentUserState(newCurrentUserState);

          new BroadcastChannel('public_auth').postMessage({
            type: 'UPDATE',
            senderExpiration: expirationState,
            senderCurrentUser: newCurrentUserState
          });
        }
    };

    verifyConnection();
  }, [currentUserState]);

  return (
    <>
      {result === undefined ? (
        <h1>Loading...</h1>
      ) : result ? (
        <>
          <h1>Spotify Connected</h1>
          <p>You may close this window</p>
        </>
      ) : (
        <>
          <h1>Spotify Not Connected</h1>
          <p>Please try again later</p>
        </>
      )}
    </>
  );
};

export default Page;