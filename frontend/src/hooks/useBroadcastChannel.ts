'use client';

import { useEffect, useRef } from "react";

type MessageHandler = (event: MessageEvent) => void;

export function useBroadcastChannel(channelName: string, onMessage: MessageHandler): BroadcastChannel | null {
  const channelRef = useRef<BroadcastChannel | null>(null);
  const channel = new BroadcastChannel(channelName);
  channelRef.current = channel;

  useEffect(() => {
    // Attach the onMessage event
    channel.onmessage = (event) => onMessage(event);

    // Return cleanup function, React treats all functions returned inside a useEffect as cleanup functions
    return () => {
      channel.close();
      channelRef.current = null;
    };
  }, [channelName, onMessage]);

  return channelRef.current;
}
