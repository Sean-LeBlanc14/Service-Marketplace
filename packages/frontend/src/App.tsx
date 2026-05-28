import { BrowserRouter, Routes, Route } from "react-router-dom";
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

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route path="homepage" element={<HomePage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="settings" element={<Settings />} />
          <Route
            path="requests"
            element={<ServiceDashboard />}
          />
          <Route path="inbox" element={<Inbox />} />
        </Route>
        <Route index element={<LandingPage />} />
        <Route path="verify" element={<VerifyAccount />} />
        <Route path="signup" element={<SignupPage />} />
        <Route path="login" element={<LoginPage />} />
      </Routes>
      <ToastContainer
        position="top-right"
        autoClose={3000}
        theme="colored"
      />
    </BrowserRouter>
  );
}

export default App;
