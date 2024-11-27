'use client';

import React, {PropsWithChildren, useContext, useLayoutEffect, useRef, useState} from "react";
import Header from "@/src/components/shared/Header";
import {User} from "@/src/types/user";
import {createContext} from "react";
import {check, login, logout, register} from "@/src/api/auth";
import {useRouter} from "next/navigation";
import {v4 as uuidv4} from 'uuid';
import {useAuthBroadcastHandlers} from "@/src/hooks/useAuthBroadcastHandlers";

type AuthContextType = {
  authSetByBroadcastRef?: React.MutableRefObject<boolean>;
  expirationState?: Date | null;
  setExpirationState: React.Dispatch<React.SetStateAction<Date | null | undefined>>;
  currentUserState?: User | null;
  setCurrentUserState: React.Dispatch<React.SetStateAction<User | null | undefined>>;
  handleRegister: (e: React.FormEvent) => Promise<void>;
  handleLogin: (e: React.FormEvent) => Promise<void>;
  handleLogout: () => Promise<void>;
  setUsername: React.Dispatch<React.SetStateAction<string>>;
  setPassword: React.Dispatch<React.SetStateAction<string>>;
  setRememberMe: React.Dispatch<React.SetStateAction<boolean>>;
  setEmail: React.Dispatch<React.SetStateAction<string>>;
  setConfirmPassword: React.Dispatch<React.SetStateAction<string>>;
  isLoadingLocal?: boolean;
  tokenExpirationTimerRef?: React.MutableRefObject<NodeJS.Timeout | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export default function AuthProvider({children}: PropsWithChildren) {
  const router = useRouter();
  const authSetByBroadcastRef = useRef(false);
  const tokenExpirationTimerRef = useRef<NodeJS.Timeout | null>(null);
  const [expirationState, setExpirationState] = useState<Date | null | undefined>(undefined);
  const [currentUserState, setCurrentUserState] = useState<User | null | undefined>(undefined);
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [rememberMe, setRememberMe] = useState<boolean>(false);
  const [email, setEmail] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoadingLocal, setIsLoadingLocal] = useState(false);
  const tabUuid = uuidv4();

  const {publicAuth} = useAuthBroadcastHandlers(tabUuid, expirationState, setExpirationState, currentUserState, setCurrentUserState, authSetByBroadcastRef, setTokenExpirationTimer);

  // Fetch the auth data on initial load
  useLayoutEffect(() => {
    fetchAuth();
  }, []);


  async function fetchAuth() {
    // Initial load needs to see if other tabs already have fetched the data
    if (currentUserState === undefined) {
      publicAuth?.postMessage({type: "AUTH_REQUEST", senderTabUUID: tabUuid});

      await new Promise((resolve) => setTimeout(resolve, 100));
      if (authSetByBroadcastRef.current) {
        return;
      }
    }

    const fetchingKey = "isFetching";
    // Prevent multiple fetches at the same time, the other tabs should return and wait for a broadcast
    //  If the broadcast is never received, user is in logged out state
    if (localStorage.getItem(fetchingKey)) {
        return;
    }

    try{
      localStorage.setItem(fetchingKey, "true");
      const response = await check();
      const {expiration, currentUser} = response ?? {expiration: null, currentUser: null};
      setExpirationState(expiration);
      setCurrentUserState(currentUser);

      if (expiration && currentUser) {
        setTokenExpirationTimer(expiration);
      }
    } finally {
      localStorage.removeItem(fetchingKey);
    }
  }

  function setTokenExpirationTimer(expiration: Date) {
    // Clear the previous timer
    if (tokenExpirationTimerRef.current) {
      clearTimeout(tokenExpirationTimerRef.current);
      tokenExpirationTimerRef.current = null;
    }

    if (expiration > new Date()) {
      tokenExpirationTimerRef.current = setTimeout(() => {
        fetchAuth();
      }, expiration?.getTime() - Date.now() - 60000); // Renew the access token one minute before expiration
    }
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();

    try {
      const response = await login(username, password, rememberMe);
      const {expiration, currentUser} = response ?? {};

      setExpirationState(expiration);
      setCurrentUserState(currentUser);
      publicAuth?.postMessage({type: "UPDATE", senderExpiration: expiration, senderCurrentUser: currentUser});

      if (expiration && currentUser) {
        setTokenExpirationTimer(expiration);
      }

      router.replace('/');
    } catch {
      router.replace('/login');
    }
  }

  async function handleLogout() {
    await logout();

    setExpirationState(null);
    setCurrentUserState(null);
    publicAuth?.postMessage({type: "UPDATE", senderExpiration: null, senderCurrentUser: null});

    if (tokenExpirationTimerRef.current) {
      clearTimeout(tokenExpirationTimerRef.current);
      tokenExpirationTimerRef.current = null;
    }

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
        authSetByBroadcastRef,
        expirationState,
        setExpirationState,
        currentUserState,
        setCurrentUserState,
        handleRegister,
        handleLogin,
        handleLogout,
        setUsername,
        setPassword,
        setRememberMe,
        setEmail,
        setConfirmPassword,
        isLoadingLocal,
        tokenExpirationTimerRef
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