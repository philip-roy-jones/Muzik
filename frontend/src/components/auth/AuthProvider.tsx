'use client';

import React, {PropsWithChildren, useContext, useRef, useState} from "react";
import {useQuery, useQueryClient} from "@tanstack/react-query";
import Header from "@/src/components/shared/Header";
import {User} from "@/src/types/user";
import {createContext} from "react";
import {check, login, logout, register} from "@/src/api/auth";
import {useRouter} from "next/navigation";
import {v4 as uuidv4} from 'uuid';
import {useAuthBroadcastHandlers} from "@/src/hooks/useAuthBroadcastHandlers";

type AuthContextType = {
  expiration?: Date | null;
  currentUser?: User | null;
  handleRegister: (e: React.FormEvent) => Promise<void>;
  handleLogin: (e: React.FormEvent) => Promise<void>;
  handleLogout: () => Promise<void>;
  setUsername: React.Dispatch<React.SetStateAction<string>>;
  setPassword: React.Dispatch<React.SetStateAction<string>>;
  setRememberMe: React.Dispatch<React.SetStateAction<boolean>>;
  setEmail: React.Dispatch<React.SetStateAction<string>>;
  setConfirmPassword: React.Dispatch<React.SetStateAction<string>>;
  isLoadingLocal?: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export default function AuthProvider({children}: PropsWithChildren) {
  const queryClient = useQueryClient();
  const router = useRouter();
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [rememberMe, setRememberMe] = useState<boolean>(false);
  const [email, setEmail] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoadingLocal, setIsLoadingLocal] = useState(false);
  const tabUuidRef = useRef<string>();
  if (tabUuidRef.current === undefined) {   // Only set the tab UUID once, persistent across refreshes
    tabUuidRef.current = uuidv4();
  }

  const {publicAuth} = useAuthBroadcastHandlers(tabUuidRef);

  async function fetchAuth(): Promise<{ expiration?: Date; currentUser?: User } | null> {
    // Initial load needs to see if other tabs already have fetched the data
    const data = queryClient.getQueryData(["auth"]);

    if (data === undefined) {
      publicAuth?.postMessage({type: "AUTH_REQUEST", senderTabUUID: tabUuidRef.current});

      await new Promise((resolve) => setTimeout(resolve, 100));

      const updatedData = queryClient.getQueryData(["auth"]);
      console.log(`Updated data: ${JSON.stringify(updatedData)}`);
      if (updatedData !== undefined) {
        return updatedData;
      }
    }

    return check();
  }

  const {data} = useQuery({
    queryKey: ["auth"],
    queryFn: fetchAuth,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const expiration = data?.expiration ?? null;
  const currentUser = data?.currentUser ?? null; // User - Logged in, null - Not logged in, undefined - Loading
  // console.log(`Expiration: ${expiration}`);
  // console.log(`Current User: ${JSON.stringify(currentUser)}`);
  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();

    try {
      const response = await login(username, password, rememberMe);
      const {expiration, currentUser} = response ?? {};

      queryClient.setQueryData(["auth"], {expiration, currentUser});
      publicAuth?.postMessage({type: "UPDATE", senderExpiration: expiration, senderCurrentUser: currentUser});

      router.replace('/');
    } catch {
      queryClient.setQueryData(["auth"], {expiration: null, currentUser: null});
      router.replace('/login');
    }
  }

  async function handleLogout() {
    await logout();

    queryClient.setQueryData(["auth"], {expiration: null, currentUser: null});
    publicAuth?.postMessage({type: "UPDATE", senderExpiration: null, senderCurrentUser: null});

    router.push('/');
  }

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      setIsLoadingLocal(true);
      const responseObject = await register(username, email, password, confirmPassword);
      console.log(responseObject);
      setIsLoadingLocal(false);
    } catch {
      setIsLoadingLocal(false);
    }
  }

  return (
    <AuthContext.Provider
      value={{
        expiration,
        currentUser,
        handleRegister,
        handleLogin,
        handleLogout,
        setUsername,
        setPassword,
        setRememberMe,
        setEmail,
        setConfirmPassword,
        isLoadingLocal
      }}
    >
      <Header/>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
}