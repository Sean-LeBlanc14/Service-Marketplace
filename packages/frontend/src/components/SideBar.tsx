import {
  FaHome,
  FaMoneyBill,
  FaSignOutAlt
} from "react-icons/fa";
import { HiUser } from "react-icons/hi2";
import { FaGear } from "react-icons/fa6";
import { FiMessageCircle } from "react-icons/fi";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import "./Styles/Sidebar.css";
import { useState } from "react";
import NavigationButton from "./NavigationButton";

export default function SideBar() {
  const navigate = useNavigate();
  const token = localStorage.getItem("jwt_token");
  const [error, setError] = useState("");

  const handleLogout = async () => {
    const confirmed = window.confirm(
      "Are you sure you want to logout?"
    );

    if (!confirmed) return;

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
      <div className="sidebar-navigation">
        <div className="logo-container">
          <p className="logo-text-serif">Poly Services</p>
        </div>

        <NavigationButton
          to={"/homepage"}
          label={"Home"}
          icon={<FaHome />}
        />

        <NavigationButton
          to={"/profile"}
          label={"Profile"}
          icon={<HiUser />}
        />

        <NavigationButton
          to={"/requests"}
          label={"Service Dashboard"}
          icon={<FaMoneyBill />}
        />

        <NavigationButton
          to="/inbox"
          label="Messages"
          icon={<FiMessageCircle />}
        />
      </div>

      <div className="sidebar-bottom">
        <NavigationButton
          to={"/settings"}
          label="Settings"
          icon={<FaGear />}
        />

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
