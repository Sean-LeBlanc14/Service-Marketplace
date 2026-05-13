import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import SignupPage from "./pages/SignupPage";
import LoginPage from "./pages/LoginPage";
import VerifyAccount from "./pages/VerifyAccount";
import ProfilePage from "./pages/ProfilePage";
import Settings from "./pages/Settings";
import ServiceRequests from "./pages/ServiceRequests";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route path="verify" element={<VerifyAccount />} />
          <Route path="homepage" element={<HomePage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="settings" element={<Settings />} />
          <Route path="requests" element={<ServiceRequests/>}/>
        </Route>
        <Route path="signup" element={<SignupPage />} />
        <Route path="login" element={<LoginPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
