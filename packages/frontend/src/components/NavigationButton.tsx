import { NavLink } from "react-router-dom";
import "./styles/NavigationButton.css";

interface NavigationButtonProps {
  to: string;
  label: string;
  icon: React.ReactNode;
  badge?: number;
}

export default function NavigationButton({
  label,
  icon,
  to,
  badge
}: NavigationButtonProps) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        isActive ? "icon-button selected" : "icon-button"
      }>
      <span className="nav-badge-wrapper">
        {icon}
        {badge != null && badge > 0 && (
          <span className="nav-badge">
            {badge > 99 ? "99+" : badge}
          </span>
        )}
      </span>
      <span>{label}</span>
    </NavLink>
  );
}
