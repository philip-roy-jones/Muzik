import LoginForm from "@/src/components/login/LoginForm";
import Link from "next/link";

export default function Login() {
  return (
    <div className="flex min-h-full flex-1 flex-col justify-center px-6 py-12 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-sm">
        <img
          alt="Muzik Logo"
          src="../favicon.ico"
          className="mx-auto h-10 w-auto"
        />
        <h2 className="mt-10 text-center text-2xl font-bold leading-9 tracking-tight">
          Sign in to your account
        </h2>
      </div>

      <div className="mt-10 sm:mx-auto sm:w-full sm:max-w-sm">
        <LoginForm/>
        <p className="mt-10 text-center text-sm text-gray-500">
          <Link href="/register" className="font-semibold leading-6 text-indigo-600 hover:text-indigo-500">
            Not a registered user?
          </Link>
        </p>
      </div>
    </div>
  );
}