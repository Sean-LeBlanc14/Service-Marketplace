import { Link } from "react-router-dom";
import "./styles/Footer.css";

function Footer() {
  return (
    <div className="app-footer">
      <span className="app-footer-text">
        © 2026 PolyService | All rights reserved
      </span>
      <span className="app-footer-links">
        <Link to="/terms" className="app-footer-link">
          Terms of Service
        </Link>
        <span className="app-footer-divider">·</span>
        <Link to="/privacy" className="app-footer-link">
          Privacy Policy
        </Link>
      </span>
    </div>
  );
}

export default Footer;
