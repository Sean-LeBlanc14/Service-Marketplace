import { Outlet } from "react-router-dom";
import SideBar from "./SideBar";
import "./styles/Layout.css";

function Layout() {
  return (
    <div className="app-wrapper-alt">
      <SideBar />

      <div className="content-area">
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default Layout;
