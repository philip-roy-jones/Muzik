'use client'

import {useState, useContext} from "react";
import Link from "next/link";
import {logout} from "@/src/api/auth";
import { useRouter } from 'next/navigation';
import {useAuth} from "@/src/components/auth/AuthProvider";

const ProfileMenu = () => {
  const [isVisible, setIsVisible] = useState(false);

  const toggleMenu = () => {
    setIsVisible(!isVisible);
  };

  const {handleLogout} = useAuth();

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