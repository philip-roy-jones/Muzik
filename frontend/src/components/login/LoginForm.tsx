"use client";

import {useAuth} from "@/src/components/auth/AuthProvider";
import { useRouter } from "next/navigation";
import { useLayoutEffect } from 'react';

export default function LoginForm() {

  const {handleLogin, setUsername, setPassword, setRememberMe, expirationState} = useAuth();

  const router = useRouter();
  useLayoutEffect(() => {
    if (expirationState) {
      router.push('/');
    }
  },[]);


  return (
    <form method="POST" className="space-y-6" onSubmit={handleLogin}>
      <div>
        <label htmlFor="username" className="block text-sm font-medium leading-6">
          Username
        </label>
        <div className="mt-2">
          <input
            id="username"
            name="username"
            type="text"
            onChange={(e) => {
              setUsername(e.target.value);
            }}
            required
            autoComplete="username"
            className="block w-full rounded-md border-0 text-gray-900 py-1.5 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"
          />
        </div>
      </div>

      <div>
        <div className="flex items-center justify-between">
          <label htmlFor="password" className="block text-sm font-medium leading-6">
            Password
          </label>
          <div className="text-sm">
            <a href="#" className="font-semibold text-indigo-600 hover:text-indigo-500">
              Forgot password?
            </a>
          </div>
        </div>
        <div className="mt-2">
          <input
            id="password"
            name="password"
            type="password"
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"
          />
        </div>
        <div className="mt-2 flex items-center">
          <input
            id="remember-me"
            name="rememberMe"
            type="checkbox"
            onChange={(e) => setRememberMe(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-600"
          />
          <label htmlFor="remember-me" className="ml-2 block text-sm">
            Remember me
          </label>
        </div>
      </div>

      <div>
        <button
          type="submit"
          className="flex w-full justify-center rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
        >
          Sign in
        </button>
      </div>
    </form>
  );
}