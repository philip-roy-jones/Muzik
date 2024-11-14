import {createContext} from "react";

interface AuthContextType {
  isLoggedIn: boolean;
  isLoggedInRef: React.MutableRefObject<boolean>;
  setIsLoggedIn: React.Dispatch<React.SetStateAction<boolean>>;

  expiryDate: Date | null;
  setExpiryDate: React.Dispatch<React.SetStateAction<Date | null>>;
}

export const AuthContext = createContext<AuthContextType | null>(null);