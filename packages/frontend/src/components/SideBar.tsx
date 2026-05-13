import { FaHome, FaMoneyBill } from "react-icons/fa";
import { HiUser } from "react-icons/hi2";
import { FaGear } from "react-icons/fa6";
import { NavLink } from "react-router-dom";
import "../Styles/Sidebar.css";

export default function SideBar() {

  return (
    <aside className="sidebar-container">

      <div className="main-container">

        <NavLink to="/homepage" className={ ({isActive}) => isActive ? "icon-button selected" : "icon-button"}>
        <FaHome/>
        <span>Home</span>
        </NavLink>

        <NavLink to="/profile" className={ ({isActive}) => isActive ? "icon-button selected" : "icon-button"}>
          <HiUser/>
          <span>Profile</span>
        </NavLink>

        <NavLink to="/requests" className={ ({isActive}) => isActive ? "icon-button selected" : "icon-button"}>
          <FaMoneyBill/>
          <span>Services</span>
        </NavLink>
        
      </div>

      <div className="settings-container">
        <NavLink to="/settings" className={ ({isActive}) => isActive ? "icon-button selected" : "icon-button"}>
          <FaGear/>
          <span>Settings</span>
        </NavLink>
      </div>

    </aside>
  );
}
