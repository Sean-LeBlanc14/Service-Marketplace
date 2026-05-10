import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "bootstrap/dist/css/bootstrap.min.css";
import App from "./App.tsx";
import ProfilePage from "./pages/ProfilePage.tsx";

const normalizedPath = window.location.pathname.replace(/\/$/, "") || "/";
const page = normalizedPath === "/profile" ? <ProfilePage /> : <App />;

createRoot(document.getElementById("root")!).render(
  <StrictMode>{page}</StrictMode>
);
