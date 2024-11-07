import {createContext} from "react";

interface AuthContextType {
  accessToken: string | null;
  setAccessToken: React.Dispatch<React.SetStateAction<string | null>>;

  expiryDate: Date | null;
  setExpiryDate: React.Dispatch<React.SetStateAction<Date | null>>;
}

export const AuthContext = createContext<AuthContextType | null>(null);