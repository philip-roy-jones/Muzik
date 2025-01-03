import axios, {AxiosResponse} from "axios";
import {User} from "@/src/types/user";

const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL;

export const register = async (username: string, email: string, password: string, confirmPassword: string) => {
  const response = await axios.post(`${backendUrl}/public/register`, {
    username,
    email,
    password,
    confirmPassword
  }, {withCredentials: true});

  return handleResponse(response);
}

// If there is one minute or less left before the token expires, the backend will automatically renew the token
export const check = async () => {
  console.log("check API request was made");
  const response = await axios.get(`${backendUrl}/public/check`, {withCredentials: true});
  return handleResponse(response);
}

export const login = async (username: string, password: string, rememberMe: boolean) => {
  const response = await axios.post(`${backendUrl}/public/login`, {
    username,
    password,
    rememberMe,
  }, {withCredentials: true});

  return handleResponse(response);
}

export const logout = async (): Promise<void> => {
  await axios.post(`${backendUrl}/public/logout`, {/* Empty Body */}, {
    withCredentials: true
  });
}


function handleResponse(response: AxiosResponse): {expiration: Date; currentUser: User} | null {
  if (response.status === 200 && !response.data.error) {
    const currentUser: User = {
      username: response.data.username,
      roles: response.data.roles,
      isSpotifyConnected: response.data.isSpotifyConnected
    }
    // This is an ISO 8601 string
    const expiration = response.data.expiration;

    // Convert to Date object
    const expirationDate = new Date(expiration);

    return {expiration: expirationDate, currentUser};
  }

  return null;
}
