"use client";

import axios from "axios";
import React, { useState } from "react";

export default function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    axios
      .post("https://localhost:8443/public/login", {
        username,
        password,
        rememberMe,
      })
      .then((response) => {
        console.log("Success:", response.data);
      })
      .catch((error) => {
        console.error("Error:", error);
      });
    console.log("Username:", username);
    console.log("Password:", password);
    console.log("Remember me:", rememberMe);
  };

  return (
    <div>
      <h1>Login</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="username">Username:</label>
          <input
            type="text"
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="password">Password:</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="rememberMe">Remember me:</label>
          <input 
            type="checkbox" 
            id="rememberMe" 
            onChange={(e) => setRememberMe(e.target.checked)}
            />
        </div>
        <button type="submit">Login</button>
      </form>
    </div>
  );
}
