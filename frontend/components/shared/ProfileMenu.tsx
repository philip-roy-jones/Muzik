'use client'

import {useState, useContext} from "react";
import Link from "next/link";
import {logout} from "@/utils/authService";
import {AuthContext} from "@/contexts/AuthContext";
import { useRouter } from 'next/navigation';

const ProfileMenu = () => {
  const [isVisible, setIsVisible] = useState(false);
  const router = useRouter();

  const toggleMenu = () => {
    setIsVisible(!isVisible);
  };

  const authContext = useContext(AuthContext);
  const { isLoggedIn, setIsLoggedIn, setExpiryDate } = authContext || {};
  const handleLogout = async () => {
    if (isLoggedIn) {
      await logout();  // Tells backend you are logging out
    }
    if (setIsLoggedIn) {
      setIsLoggedIn(false);  // Clears the token from memory
    }

    if (setExpiryDate) {
      setExpiryDate(null);  // Clears the expiry date from memory
    }
    router.push('/login');
  }

  return (
    <div className="relative">
      <button onClick={toggleMenu} className="profile-button">
        Profile
      </button>
      {isVisible && (
        <div className="absolute right-0 mt-2 w-48 bg-white border rounded shadow-lg">
          <ul>
            <li>
              <Link href="/profile" className="block text-left px-4 py-2 hover:bg-gray-200">
                Show Profile
              </Link>
            </li>
            <li>
              <button className="block w-full text-left px-4 py-2 hover:bg-gray-200" onClick={handleLogout}>
                Logout
              </button>
            </li>
          </ul>
        </div>
      )}
    </div>
  );
};

export default ProfileMenu;