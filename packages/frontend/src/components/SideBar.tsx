import {
  FaHome,
  FaMoneyBill,
  FaUserShield,
  FaSignOutAlt
} from "react-icons/fa";
import { HiUser } from "react-icons/hi2";
import { FaGear } from "react-icons/fa6";
import { FiMessageCircle } from "react-icons/fi";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import "../styles/global.css";
import "./styles/SideBar.css";
import NavigationButton from "./NavigationButton";
import { API_ENDPOINTS } from "../utils/api";

export default function SideBar() {
  const navigate = useNavigate();
  const isAdmin = localStorage.getItem("user_role") === "admin";
  

  const handleLogout = async () => {
    const confirmed = window.confirm(
      "Are you sure you want to logout?"
    );

    if (!confirmed) return;

    try {
      const token = localStorage.getItem("jwt_token");
      const response = await fetch(API_ENDPOINTS.auth.logout, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      if (response.ok) {
        localStorage.removeItem("jwt_token");
        localStorage.removeItem("user_role");
        localStorage.removeItem("user_id");
        toast.success("Successfully logged out");
        navigate("/login");
      } else if (response.status === 401) {
        // Backend sends a 401 if the user is not logged in to begin with
        localStorage.removeItem("jwt_token");
        localStorage.removeItem("user_role");
        localStorage.removeItem("user_id");
        navigate("/login");
      } else {
        toast.error("Could not logout, please try again.");
      }
    } catch (e) {
      console.error("Logout error: ", e);
      toast.warning("A network error occurred.");
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
          to={"/dashboard"}
          label={"Service Dashboard"}
          icon={<FaMoneyBill />}
        />

        {isAdmin && (
          <NavigationButton
            to={"/admin"}
            label={"Admin"}
            icon={<FaUserShield />}
          />
        )}

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
