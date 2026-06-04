import { useNavigate } from "react-router-dom";
import "../styles/PolicyPage.css";

function TermsOfService() {
  const navigate = useNavigate();

  return (
    <div className="policy-page">
      <div className="policy-container">
        <button
          className="policy-back-btn"
          onClick={() => navigate(-1)}>
          &larr; Back
        </button>

        <h1 className="policy-title">Terms of Service</h1>
        <p className="policy-last-updated">
          Last updated: June 4, 2026
        </p>

        <section className="policy-section">
          <h2>1. Acceptance of Terms</h2>
          <p>
            By creating an account or using PolyServices, you
            agree to be bound by these Terms of Service. If you
            do not agree, do not use the platform.
          </p>
        </section>

        <section className="policy-section">
          <h2>2. Eligibility</h2>
          <p>
            PolyServices is exclusively available to currently
            enrolled Cal Poly students. Registration requires a
            valid <strong>@calpoly.edu</strong> email address.
            You are responsible for keeping your account
            credentials secure. You may not share, transfer, or
            sell your account.
          </p>
        </section>

        <section className="policy-section">
          <h2>3. Services and Listings</h2>
          <p>
            Users may list and purchase student-to-student
            services within categories provided by the platform.
            By posting a listing, you represent that you have
            the ability and intent to fulfill the service as
            described. Listings must not:
          </p>
          <ul>
            <li>
              Violate any Cal Poly policy, local, state, or
              federal law
            </li>
            <li>
              Involve the sale of controlled substances,
              weapons, or illegal goods
            </li>
            <li>
              Constitute academic dishonesty as defined by the
              Cal Poly Academic Integrity Policy
            </li>
            <li>
              Discriminate based on race, gender, religion,
              disability, or any other protected characteristic
            </li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>4. Payments and Transactions</h2>
          <p>
            All payments are processed securely through our
            third-party payment provider, Stripe. PolyServices
            does not store payment card details. Buyers and
            sellers are responsible for agreeing on the scope
            and price of a service. PolyServices is not a party
            to any transaction between users and does not
            guarantee the quality, safety, or legality of any
            service listed.
          </p>
        </section>

        <section className="policy-section">
          <h2>5. Price Negotiation</h2>
          <p>
            The platform provides a chat-based negotiation
            feature. Any price agreed upon through the chat
            system is binding between the buyer and seller.
            PolyServices is not responsible for disputes arising
            from informal agreements made outside the platform.
          </p>
        </section>

        <section className="policy-section">
          <h2>6. Account Suspension and Termination</h2>
          <p>
            PolyServices reserves the right to suspend or
            terminate accounts that violate these Terms, engage
            in fraudulent activity, or abuse the platform.
            Suspended users may not create new accounts. If your
            account is suspended, you will be notified via your
            registered email.
          </p>
        </section>

        <section className="policy-section">
          <h2>7. Disclaimer of Warranties</h2>
          <p>
            PolyServices is provided "as is" without warranties
            of any kind. We do not guarantee uninterrupted
            access, error-free operation, or the quality of any
            service listed by users.
          </p>
        </section>

        <section className="policy-section">
          <h2>8. Limitation of Liability</h2>
          <p>
            To the maximum extent permitted by law, PolyServices
            and its operators shall not be liable for any
            indirect, incidental, or consequential damages
            arising from your use of the platform, including
            disputes between buyers and sellers.
          </p>
        </section>

        <section className="policy-section">
          <h2>9. Changes to These Terms</h2>
          <p>
            We may update these Terms from time to time.
            Continued use of the platform after changes are
            posted constitutes acceptance of the revised Terms.
          </p>
        </section>

        <section className="policy-section">
          <h2>10. Contact</h2>
          <p>
            For questions about these Terms, contact us at{" "}
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

export default TermsOfService;
