import type { ConnectStatus } from "./types";

interface PaymentsSectionProps {
  connectStatus: ConnectStatus | null;
  isConnecting: boolean;
  onConnectStripe: () => void;
}

export function PaymentsSection({
  connectStatus,
  isConnecting,
  onConnectStripe
}: PaymentsSectionProps) {
  return (
    <section className="profile-section" aria-label="Payments">
      <div className="section-heading">
        <div>
          <h2>Payments</h2>
          <p>
            Connect Stripe to receive payments for your services.
          </p>
        </div>
        {connectStatus && !connectStatus.chargesEnabled && (
          <button
            type="button"
            className="section-action-button"
            disabled={isConnecting}
            onClick={onConnectStripe}>
            {isConnecting
              ? "Redirecting..."
              : connectStatus.accountId
                ? "Continue Setup"
                : "Connect Stripe"}
          </button>
        )}
      </div>
      {connectStatus?.chargesEnabled ? (
        <p className="connect-status connect-status--active">
          Stripe connected. Payments are active.
        </p>
      ) : connectStatus?.detailsSubmitted ? (
        <p className="connect-status">
          Stripe setup in progress. Payments will be enabled once
          verification is complete.
        </p>
      ) : (
        <p className="empty-state">
          Connect a Stripe account to receive payments from
          customers.
        </p>
      )}
    </section>
  );
}
