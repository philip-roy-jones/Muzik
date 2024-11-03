'use client';

import {AuthContext} from "@/contexts/AuthContext";
import {useEffect, useState} from "react";
import {renewTokens} from "@/utils/authService";
import Header from "@/components/shared/Header";

export default function AuthProvider({children,}: Readonly<{ children: React.ReactNode; }>) {
  const [accessToken, setAccessToken] = useState<string | null>(null);

  useEffect(() => {
    const fetchTokens = async () => {
      const accessToken = await renewTokens();

      if (accessToken === null) {
        console.error("Invalid refresh token");
        return;
      } else {
        setAccessToken(accessToken);
      }
    };
    fetchTokens();
  }, []);

  return (
    <AuthContext.Provider value={{accessToken, setAccessToken}}>
      <Header />
      {children}
    </AuthContext.Provider>
  );
}