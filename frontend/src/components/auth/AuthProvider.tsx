'use client';

import React, {PropsWithChildren, useContext, useState} from "react";
import { useQuery, QueryClient, QueryClientProvider } from "react-query";
import Header from "@/src/components/shared/Header";
import {User} from "@/src/types/user";
import {createContext} from "react";
import {login, logout, register} from "@/src/api/auth";
import {useRouter} from "next/navigation";

type AuthContextType = {
  accessToken?: string | null;
  currentUser?: User | null;
  handleRegister: (e: React.FormEvent) => Promise<void>;
  handleLogin: (e: React.FormEvent) => Promise<void>;
  handleLogout: () => Promise<void>;
  setUsername: React.Dispatch<React.SetStateAction<string>>;
  setPassword: React.Dispatch<React.SetStateAction<string>>;
  setRememberMe: React.Dispatch<React.SetStateAction<boolean>>;
  setEmail: React.Dispatch<React.SetStateAction<string>>;
  setConfirmPassword: React.Dispatch<React.SetStateAction<string>>;
  isLoading?: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);
const queryClient = new QueryClient();

export default function AuthProvider({children}: PropsWithChildren) {
  const [accessToken, setAccessToken] = useState<string | null>(); // default undefined
  const [currentUser, setCurrentUser] = useState<User | null>(); // default undefined
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [rememberMe, setRememberMe] = useState<boolean>(false);
  const [email, setEmail] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const router = useRouter();

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();

    try {
      const response = await login(username, password, rememberMe);
      const {accessToken, currUser} = response[1] as { accessToken: string; currUser: User };

      setAccessToken(accessToken);
      setCurrentUser(currUser);
      router.push('/');
    } catch {
      setAccessToken(null);
      setCurrentUser(null);
      router.replace('/login');
    }
  }

  async function handleLogout() {
    await logout();

    setAccessToken(null);
    setCurrentUser(null);
    router.push('/');
  }

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      setIsLoading(true);
      const responseObject = await register(username, email, password, confirmPassword);
      console.log(responseObject);
      setIsLoading(false);
    } catch {
      setIsLoading(false);
    }
  }

  return (
    <AuthContext.Provider
      value={{
        accessToken,
        currentUser,
        handleRegister,
        handleLogin,
        handleLogout,
        setUsername,
        setPassword,
        setRememberMe,
        setEmail,
        setConfirmPassword,
        isLoading
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