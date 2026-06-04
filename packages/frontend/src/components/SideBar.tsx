import {
  FaBell,
  FaCalendarAlt,
  FaHome,
  FaMoneyBill,
  FaUserShield
} from "react-icons/fa";
import { HiUser } from "react-icons/hi2";
import { FaGear } from "react-icons/fa6";
import { FiMessageCircle } from "react-icons/fi";
import "../styles/global.css";
import "./styles/SideBar.css";
import NavigationButton from "./NavigationButton";
import { useWebSocketContext } from "../context/WebSocketContext";

export default function SideBar() {
  const isAdmin = localStorage.getItem("user_role") === "admin";
  const { unreadCounts } = useWebSocketContext();

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
          to="/calendar"
          label="Calendar"
          icon={<FaCalendarAlt/>}
        />

        <NavigationButton
          to="/inbox"
          label="Messages"
          icon={<FiMessageCircle />}
          badge={
            unreadCounts.messages > 0
              ? unreadCounts.messages
              : undefined
          }
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
          label=""
          icon={<FaBell />}
          badge={
            unreadCounts.notifications > 0
              ? unreadCounts.notifications
              : undefined
          }
        />
      </div>
    </aside>
  );
}
