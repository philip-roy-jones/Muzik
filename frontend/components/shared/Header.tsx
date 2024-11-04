import Link from "next/link";
import ProfileButton from "@/components/shared/ProfileButton";

function Header() {

  return (
    <nav className={"flex justify-between px-4 sm:px-8 md:px-16 lg:px-32 xl:px-40 mt-3"}>
      <div className={"inline w-1/12"}>
        <Link href="/" className="brand-logo">
          Muzik
        </Link>
      </div>

      <div className={"inline w-full text-center"}>
        <ul className="">
          <li><Link href={"#"}>Spotify</Link></li>
        </ul>
      </div>

      <div className={"inline w-1/12 text-right"}>
        <ProfileButton />
      </div>
    </nav>
  );
}

export default Header;