import { useNavigate } from "react-router-dom";
import SubmitButton from "../components/SubmitButton";
import "../Styles/LandingPage.css";
import "../Styles/global.css";

function LandingPage() {
    const navigate = useNavigate();

    return (
        <div className="landing-container">
            <div className="hero-overlay"></div>

            {/* Main Content Wrapper */}
            <div className="content">

                <main className="main-hero">
                    <h1 className="main-title">
                        YOUR CAMPUS. <br />
                        <span className="highlight-text">YOUR MARKETPLACE.</span>
                    </h1>
                    
                    <p className="subtitle">
                        PolyServices connects you with verified students for specialized help, services, and gear right here at Cal Poly. No outsiders. Just Mustangs helping Mustangs.
                    </p>

                    <div className="feature-grid">
                        <div className="feature-item">
                            <h3 className="feature-title">VERIFIED STUDENTS</h3>
                            <p className="feature-desc">
                                Exclusive to the Mustang community. Secure, student-only access via your university credentials.
                            </p>
                        </div>
                        
                        <div className="feature-item">
                            <h3 className="feature-title">ON CAMPUS</h3>
                            <p className="feature-desc">
                                Find specialized help, textbooks, and gear just a short walk from your dorm or library.
                            </p>
                        </div>
                        
                        <div className="feature-item">
                            <h3 className="feature-title">TAILORED CATEGORIES</h3>
                            <p className="feature-desc">
                                Built specifically for student life. Categories tailored directly to your major and dorm living needs.
                            </p>
                        </div>
                    </div>

                    <div className="landing-action-buttons">
                        <SubmitButton 
                            label="Join us" 
                            onClick={() => navigate("/signup")}
                        />
                        <SubmitButton 
                            label="Login" 
                            onClick={() => navigate("/login")}
                        />
                    </div>
                </main>

                <footer className="landing-footer">
                    <p>© 2026 PolyServices, Inc. All Rights Reserved.</p>
                </footer>
            </div>
        </div>
    );
}

export default LandingPage;