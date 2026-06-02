import {
  FaBell,
  FaHome,
  FaMoneyBill,
  FaUserShield,
} from "react-icons/fa";
import { HiUser } from "react-icons/hi2";
import { FaGear } from "react-icons/fa6";
import { FiMessageCircle } from "react-icons/fi";
import "../styles/global.css";
import "./styles/SideBar.css";
import NavigationButton from "./NavigationButton";

export default function SideBar() {
  const isAdmin = localStorage.getItem("user_role") === "admin";

  

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

        <NavigationButton
          to="/notifications"
          icon={<FaBell/>}
        />

        
      </div>
    </aside>
  );
}
