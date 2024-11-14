import axios, {AxiosResponse} from "axios";

interface TokenResponse {
  expSeconds: number,
  loginStatus: boolean
}

export const register= async (username: string, email: string, password: string, confirmPassword: string): Promise<TokenResponse | null> => {
  const response = await axios.post("https://localhost:8443/public/register", {
    username,
    email,
    password,
    confirmPassword
  }, {withCredentials: true});

  return handleResponse(response);
}

export const checkTokens = async () => {
  const response = await axios.get("https://localhost:8443/public/check", {withCredentials: true});
  return handleResponse(response);
}

export const renewTokens = async (): Promise<TokenResponse|null> => {
  // Backend automatically sets refresh token in cookie
  const response = await axios.post("https://localhost:8443/public/refresh",
    {/* Empty Body */}, {withCredentials: true});

  return handleResponse(response);
}

export const login = async (username: string, password: string, rememberMe: boolean): Promise<TokenResponse | null> => {
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






function handleResponse(response: AxiosResponse): TokenResponse | null {
  if (response.status === 200) {
    if (response.data.error) {
      return null;
    } else {
      return {
        expSeconds: response.data.accessTokenExpiration,
        loginStatus: response.data.isLoggedIn
      };
    }
  } else if (response.status === 400 || response.status === 401) {
    throw new Error("Error: Bad Request");
  } else {
    throw new Error("Error: Unknown");
  }
}
