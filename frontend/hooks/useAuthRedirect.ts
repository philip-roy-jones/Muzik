import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

// Using accessTokenRef to get the most up-to-date value of the access token
export function useAuthRedirect(isLoggedIn:boolean, pathname: string, router: ReturnType<typeof useRouter>) {
  useEffect(() => {
    if (!isLoggedIn) { // No access token, user is not logged in
      // Block all routes until accessToken is set (except login and register)
      // console.log("No access token, user is not logged in");
      const isLoginOrRegisterRoute = pathname === '/login' || pathname === '/register' || pathname === '/debug';
      if (!isLoginOrRegisterRoute) {
        router.push('/login');
      }
    } else { // User is logged in
      // console.log("User is logged in");
      const isLoginOrRegisterRoute = pathname === '/login' || pathname === '/register';
      if (isLoginOrRegisterRoute) {
        // console.log('User is logged in, redirecting to home');
        router.push('/'); // Redirect to home or another appropriate page
      }
    }
  }, [isLoggedIn, pathname, router]);
}