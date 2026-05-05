import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import SignupPage from "./pages/SignupPage";
import LoginPage from "./pages/LoginPage";

function App() {
  return (
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route path="signup" element={<SignupPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route index element={<HomePage />} />
          </Route>
        </Routes>
      </BrowserRouter>
  );
}

export default App;
