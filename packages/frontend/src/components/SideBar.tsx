import {
  FaHome,
  FaMoneyBill,
  FaSignOutAlt
} from "react-icons/fa";
import { HiUser } from "react-icons/hi2";
import { FaGear } from "react-icons/fa6";
import { NavLink, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import "../Styles/Sidebar.css";
import { useState } from "react";

export default function SideBar() {
  const navigate = useNavigate();
  const token = localStorage.getItem("jwt_token");
  const [error, setError] = useState("");

  const handleLogout = async () => {
    try {
      const response = await fetch(
        "http://localhost:8080/api/auth/logout",
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      );

      if (response.ok) {
        localStorage.removeItem("jwt_token");
        toast.success("Successfully logged out");
        navigate("/login");
      } else {
        setError("Could not logout, please try again.");
      }

      if (error) {
        toast.error(error);
      }
    } catch (e) {
      console.error("Logout error: ", e);
      toast.error("A network error occurred.");
    }
  };

  return (
    <aside className="sidebar-container">
      <div className="main-container">
        <NavLink
          to="/homepage"
          className={({ isActive }) =>
            isActive ? "icon-button selected" : "icon-button"
          }>
          <FaHome />
          <span>Home</span>
        </NavLink>

        <NavLink
          to="/profile"
          className={({ isActive }) =>
            isActive ? "icon-button selected" : "icon-button"
          }>
          <HiUser />
          <span>Profile</span>
        </NavLink>

        <NavLink
          to="/requests"
          className={({ isActive }) =>
            isActive ? "icon-button selected" : "icon-button"
          }>
          <FaMoneyBill />
          <span>Services</span>
        </NavLink>
      </div>

      <div className="settings-container">
        <NavLink
          to="/settings"
          className={({ isActive }) =>
            isActive ? "icon-button selected" : "icon-button"
          }>
          <FaGear />
          <span>Settings</span>
        </NavLink>

        <button
          className="logout-button"
          onClick={handleLogout}>
          <FaSignOutAlt />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
}
