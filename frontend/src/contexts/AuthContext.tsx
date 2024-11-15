import {createContext} from "react";

interface AuthContextType {
  username: string;
  usernameRef: React.MutableRefObject<string>;
  setUsername: React.Dispatch<React.SetStateAction<string>>;

  roles: string[];
  setRoles: React.Dispatch<React.SetStateAction<string[]>>;
}

export const AuthContext = createContext<AuthContextType | null>(null);