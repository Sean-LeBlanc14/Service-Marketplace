import { useNavigate } from "react-router-dom";
import "../styles/PolicyPage.css";

function PrivacyPolicy() {
  const navigate = useNavigate();

  return (
    <div className="policy-page">
      <div className="policy-container">
        <button
          className="policy-back-btn"
          onClick={() => navigate(-1)}>
          &larr; Back
        </button>

        <h1 className="policy-title">Privacy Policy</h1>
        <p className="policy-last-updated">
          Last updated: June 4, 2026
        </p>

        <section className="policy-section">
          <h2>1. Overview</h2>
          <p>
            PolyServices is committed to protecting your
            privacy. This Privacy Policy explains what
            information we collect, how we use it, and your
            rights regarding your data.
          </p>
        </section>

        <section className="policy-section">
          <h2>2. Information We Collect</h2>
          <p>
            When you create an account or use PolyServices, we
            collect:
          </p>
          <ul>
            <li>
              <strong>Account information:</strong> first name,
              last name, Cal Poly email address, campus, and
              major
            </li>
            <li>
              <strong>Profile information:</strong> profile
              photo, bio, and any service listings you create
            </li>
            <li>
              <strong>Transaction data:</strong> booking
              history, payment confirmations (payment card
              details are handled exclusively by Stripe and
              never stored by us)
            </li>
            <li>
              <strong>Communications:</strong> messages sent
              through the platform's chat and negotiation
              features
            </li>
            <li>
              <strong>Usage data:</strong> pages visited,
              features used, and interaction timestamps
            </li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>3. How We Use Your Information</h2>
          <p>We use your information to:</p>
          <ul>
            <li>
              Verify your Cal Poly student status and maintain
              your account
            </li>
            <li>
              Connect you with other students for services and
              transactions
            </li>
            <li>
              Process payments securely through our payment
              provider
            </li>
            <li>
              Send transactional emails (booking confirmations,
              account verification, notifications)
            </li>
            <li>
              Enforce our Terms of Service and protect the
              safety of the community
            </li>
            <li>
              Improve the platform based on usage patterns
            </li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>4. Sharing of Information</h2>
          <p>
            We do not sell your personal information. We share
            data only in the following limited circumstances:
          </p>
          <ul>
            <li>
              <strong>With other users:</strong> your public
              profile information (name, major, bio, listings)
              is visible to other verified Cal Poly students
            </li>
            <li>
              <strong>Payment processing:</strong> Stripe
              receives payment information necessary to complete
              transactions
            </li>
            <li>
              <strong>Email delivery:</strong> SendGrid is used
              to deliver transactional emails on our behalf
            </li>
            <li>
              <strong>Legal requirements:</strong> if required
              by law or to protect the rights and safety of our
              users
            </li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>5. Data Storage and Security</h2>
          <p>
            Your data is stored in MongoDB Atlas, a cloud
            database service with encryption at rest and in
            transit. We implement industry-standard security
            practices including JWT-based authentication.
            Despite these measures, no system is completely
            secure, and we cannot guarantee absolute security.
          </p>
        </section>

        <section className="policy-section">
          <h2>6. Your Rights</h2>
          <p>You have the right to:</p>
          <ul>
            <li>
              Access the personal information we hold about you
            </li>
            <li>
              Request correction of inaccurate information
            </li>
            <li>
              Request deletion of your account and associated
              data
            </li>
            <li>
              Opt out of non-essential communications via your
              account settings
            </li>
          </ul>
          <p>
            To exercise these rights, contact us at{" "}
            <a href="mailto:support@polyservices.app">
              support@polyservices.app
            </a>
            .
          </p>
        </section>

        <section className="policy-section">
          <h2>7. Cookies and Local Storage</h2>
          <p>
            PolyServices uses browser local storage to maintain
            your session (authentication token and user
            preferences). We do not use third-party tracking
            cookies.
          </p>
        </section>

        <section className="policy-section">
          <h2>8. Changes to This Policy</h2>
          <p>
            We may update this Privacy Policy periodically.
            Significant changes will be communicated via email
            or an in-app notice. Continued use of the platform
            after changes constitutes acceptance of the updated
            policy.
          </p>
        </section>

        <section className="policy-section">
          <h2>9. Contact</h2>
          <p>
            For privacy-related questions or requests, contact
            us at{" "}
            <a href="mailto:support@polyservices.app">
              support@polyservices.app
            </a>
            .
          </p>
        </section>
      </div>
    </div>
  );
}

export default PrivacyPolicy;
