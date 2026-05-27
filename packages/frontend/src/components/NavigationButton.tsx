import { NavLink } from "react-router-dom";
import "./styles/NavigationButton.css";

interface NavigationButtonProps {
  to: string;
  label: string;
  icon: React.ReactNode;
}

export default function NavigationButton({
  label,
  icon,
  to
}: NavigationButtonProps) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        isActive ? "icon-button selected" : "icon-button"
      }>
      {icon}
      <span>{label}</span>
    </NavLink>
  );
}
