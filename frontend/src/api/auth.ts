import axios, {AxiosResponse} from "axios";
import {User} from "@/src/types/user";

export const register= async (username: string, email: string, password: string, confirmPassword: string) => {
  const response = await axios.post("https://localhost:8443/public/register", {
    username,
    email,
    password,
    confirmPassword
  }, {withCredentials: true});

  return handleResponse(response);
}

export const check = async () => {
  const response = await axios.get("https://localhost:8443/public/check", {withCredentials: true});
  return handleResponse(response);
}

export const renewTokens = async () => {
  // Backend automatically sets refresh token in cookie
  const response = await axios.post("https://localhost:8443/public/refresh",
    {/* Empty Body */}, {withCredentials: true});

  return handleResponse(response);
}

export const login = async (username: string, password: string, rememberMe: boolean) => {
  const response = await axios.post("https://localhost:8443/public/login", {
    username,
    password,
    rememberMe,
  }, {withCredentials: true});

  return handleResponse(response);
}

export const logout = async (): Promise<void> => {
  await axios.post("https://localhost:8443/public/logout", {/* Empty Body */}, {
    withCredentials: true
  });
}






function handleResponse(response: AxiosResponse) {
  if (response.status === 200) {
    if (response.data.error) {
      return [400, null];
    } else {
      const currUser: User = {
        username: response.data.username,
        roles: response.data.roles
      }
      const accessToken = response.data.accessToken;

      return [200, {accessToken, currUser}] as const;
    }
  } else if (response.status === 400 || response.status === 401) {
    throw new Error("Error: Bad Request");
  } else {
    throw new Error("Error: Unknown");
  }
}
