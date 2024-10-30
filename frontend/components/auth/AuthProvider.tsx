'use client';

import axios from "axios";
import { useEffect } from "react";

export default function AuthProvider() {
  const refreshToken = async () => {
    try {
      const response = await axios.post(
        "https://localhost:8443/public/refresh",
        {},
        {
          withCredentials: true,
        }
      );
      console.log("Refreshed token:", response.data);
    } catch (error) {
      console.error("Failed to refresh token:", error);
    }
  };

  useEffect(() => {
    const fetchData = async () => {
      await refreshToken();
    };
    fetchData();
  }, []);

  return null;
}