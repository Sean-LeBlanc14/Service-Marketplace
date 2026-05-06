import { Link } from "react-router-dom";

interface NavLinkProps {
  to: string;
  label: string;
}

export default function NavLink({
  to,
  label
}: NavLinkProps) {
  return (
    <Link to={to} style={{ fontSize: "0.9rem", color: "#003831" }}>
      {label}
    </Link>
  );
}
