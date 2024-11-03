import axios, {AxiosResponse} from "axios";

export const renewTokens = async (): Promise<string|null> => {
  // Backend automatically sets refresh token in cookie
  const response = await axios.post("https://localhost:8443/public/refresh",
    {/* Empty Body */}, {withCredentials: true});

  return handleResponse(response);
}

export const login = async (username: string, password: string, rememberMe: boolean): Promise<string | null> => {
  const response = await axios.post("https://localhost:8443/public/login", {
    username,
    password,
    rememberMe,
  }, {withCredentials: true});

  return handleResponse(response);
}

export const logout = async (accessToken: string): Promise<void> => {
  await axios.post("https://localhost:8443/public/logout", {/* Empty Body */}, {
    withCredentials: true,
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
}

function handleResponse(response: AxiosResponse): string | null {
  if (response.status === 200) {
    if (response.data.error) {
      return null;
    } else {
      return response.data.accessToken;
    }
  } else if (response.status === 400 || response.status === 401) {
    throw new Error("Error: Bad Request");
  } else {
    throw new Error("Error: Unknown");
  }
}
