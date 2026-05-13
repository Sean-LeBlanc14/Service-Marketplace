import { Outlet } from "react-router-dom";
import SideBar from "./SideBar";
import NavBar from "./NavBar";
import "../Styles/Layout.css";

function Layout() {
  return (
    <div className="app-wrapper">
      <header className="navbar-wrapper">
        <NavBar />
      </header>

      <div className="layout-container">
        <aside className="sidebar-wrapper">
          <SideBar />
        </aside>

        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default Layout;
