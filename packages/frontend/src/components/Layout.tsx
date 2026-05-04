import { Outlet } from "react-router-dom";

function Layout() {
    return (
        <div>
                {/*<h1>Service Marketplace</h1>*/}
            <main>
                <Outlet />
            </main>
        </div>
    );
}

export default Layout;