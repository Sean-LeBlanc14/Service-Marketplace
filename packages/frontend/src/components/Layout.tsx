import { Outlet } from "react-router-dom";
import SideBar from "./SideBar";
import NavBar from "./NavBar";
import "./Styles/Layout.css";

function Layout() {
  return (
    <div className="app-wrapper-alt">
      <SideBar />

      <div className="content-area">
        <NavBar />
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default Layout;
