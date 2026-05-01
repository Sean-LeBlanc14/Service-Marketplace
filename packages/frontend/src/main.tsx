import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import SignUp from "./SignUp.tsx";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <SignUp />
  </StrictMode>
);
