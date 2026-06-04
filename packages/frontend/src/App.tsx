import { useEffect, useState } from "react";
import {
  BrowserRouter,
  Navigate,
  Routes,
  Route,
  useLocation
} from "react-router-dom";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import SignupPage from "./pages/SignupPage";
import LoginPage from "./pages/LoginPage";
import VerifyAccount from "./pages/VerifyAccount";
import ProfilePage from "./pages/ProfilePage";
import ProviderProfilePage from "./pages/ProviderProfilePage";
import Settings from "./pages/Settings";
import { ToastContainer } from "react-toastify";
import Inbox from "./pages/Inbox";
import LandingPage from "./pages/LandingPage";
import ServiceDashboard from "./pages/ServiceDashboard";
import AdminDashboard from "./pages/AdminDashboard";
import SuspendedPage from "./pages/SuspendedPage";
import NotificationsPage from "./pages/NotificationsPage";
import TermsOfService from "./pages/TermsOfService";
import PrivacyPolicy from "./pages/PrivacyPolicy";
import { API_ENDPOINTS } from "./utils/api";
import { WebSocketProvider } from "./context/WebSocketContext";
import Calendar from "./pages/Calendar";

const TOKEN_STORAGE_KEY = "jwt_token";

function AppRoutes() {
  const location = useLocation();
  const [serverSuspended, setServerSuspended] = useState(false);
  const isSuspended =
    localStorage.getItem("user_role") === "suspended";
  const canAccessWhileSuspended =
    location.pathname === "/login" ||
    location.pathname === "/signup" ||
    location.pathname === "/suspended";

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    if (!token) {
      return;
    }

    let cancelled = false;

    async function checkSuspension() {
      try {
        const response = await fetch(
          API_ENDPOINTS.user.profile,
          {
            headers: {
              Authorization: `Bearer ${token}`
            }
          }
        );

        if (!response.ok) {
          return;
        }

        const user = (await response.json()) as {
          role?: string;
        };

        if (!cancelled && user.role === "suspended") {
          localStorage.clear();
          setServerSuspended(true);
        }
      } catch {
        // Keep routing unchanged when the profile check cannot complete.
      }
    }

    void checkSuspension();
    const interval = setInterval(
      () => void checkSuspension(),
      300000
    );

    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  if (serverSuspended) {
    return <Navigate to="/suspended" replace />;
  }

  if (isSuspended && !canAccessWhileSuspended) {
    return <Navigate to="/suspended" replace />;
  }

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route path="homepage" element={<HomePage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route
          path="providers/:userId"
          element={<ProviderProfilePage />}
        />
        <Route path="settings" element={<Settings />} />
        <Route
          path="dashboard"
          element={<ServiceDashboard />}
        />
        <Route path="/calendar" element={<Calendar />} />
        <Route path="inbox" element={<Inbox />} />
        <Route
          path="notifications"
          element={<NotificationsPage />}
        />
        <Route path="admin" element={<AdminDashboard />} />
      </Route>
      <Route index element={<LandingPage />} />
      <Route path="verify" element={<VerifyAccount />} />
      <Route path="signup" element={<SignupPage />} />
      <Route path="login" element={<LoginPage />} />
      <Route path="suspended" element={<SuspendedPage />} />
      <Route path="terms" element={<TermsOfService />} />
      <Route path="privacy" element={<PrivacyPolicy />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <WebSocketProvider>
        <AppRoutes />
        <ToastContainer
          position="top-right"
          autoClose={3000}
          theme="colored"
        />
      </WebSocketProvider>
    </BrowserRouter>
  );
}

export default App;
