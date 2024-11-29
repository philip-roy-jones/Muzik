'use client';

import {useLayoutEffect, useRef} from "react";

type MessageHandler = (event: MessageEvent) => void;

export function useBroadcastChannel(channelName: string, onMessage: MessageHandler): BroadcastChannel | null {
  const channelRef = useRef<BroadcastChannel | null>(null);
  const channel = new BroadcastChannel(channelName);
  channelRef.current = channel;

  useLayoutEffect(() => {
    // Attach the onMessage event
    channel.onmessage = (event) => onMessage(event);

    // Return cleanup function, React treats all functions returned inside a useEffect as cleanup functions
    return () => {
      channel.close();
      channelRef.current = null;
      console.log("Clean up function called");
    };
  }, []);

  return channelRef.current;
}
