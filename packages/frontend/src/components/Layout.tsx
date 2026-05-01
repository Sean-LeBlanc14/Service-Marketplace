import { Outlet } from "react-router-dom";

function Layout() {
    return (
        <div>
        <nav>
            <h1>Service Marketplace</h1>
        </nav>
        <main>
            <Outlet />
        </main>
        </div>
    );
}

export default Layout;