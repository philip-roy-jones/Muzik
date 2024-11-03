import RegisterForm from "@/components/register/RegisterForm";
import Link from "next/link";


export default function Register() {
  return (
    <>
      <Link href={"/"}>Home</Link>
      <RegisterForm />
    </>
  );
}
