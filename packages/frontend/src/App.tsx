import { BrowserRouter, Navigate, Routes, Route, useLocation } from "react-router-dom";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import SignupPage from "./pages/SignupPage";
import LoginPage from "./pages/LoginPage";
import VerifyAccount from "./pages/VerifyAccount";
import ProfilePage from "./pages/ProfilePage";
import Settings from "./pages/Settings";
import { ToastContainer } from "react-toastify";
import Inbox from "./pages/Inbox";
import LandingPage from "./pages/LandingPage";
import ServiceDashboard from "./pages/ServiceDashboard";
import AdminDashboard from "./pages/AdminDashboard";
import SuspendedPage from "./pages/SuspendedPage";

function AppRoutes() {
  const location = useLocation();
  const isSuspended = localStorage.getItem("user_role") === "suspended";
  const canAccessWhileSuspended =
    location.pathname === "/login" ||
    location.pathname === "/signup" ||
    location.pathname === "/suspended";

  if (isSuspended && !canAccessWhileSuspended) {
    return <Navigate to="/suspended" replace />;
  }

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route path="homepage" element={<HomePage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="settings" element={<Settings />} />
        <Route
          path="dashboard"
          element={<ServiceDashboard />}
        />
        <Route path="inbox" element={<Inbox />} />
        <Route path="admin" element={<AdminDashboard />} />
      </Route>
      <Route index element={<LandingPage/>}/>
      <Route path="verify" element={<VerifyAccount />} />
      <Route path="signup" element={<SignupPage />} />
      <Route path="login" element={<LoginPage />} />
      <Route path="suspended" element={<SuspendedPage />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
      <ToastContainer
        position="top-right"
        autoClose={3000}
        theme="colored"
      />
    </BrowserRouter>
  );
}

export default App;
