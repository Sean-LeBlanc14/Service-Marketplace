import { useState } from "react";
import InputField from "./InputField";
import SubmitButton from "./SubmitButton";
import PaymentForm from "./PaymentForm";
import "./styles/ServiceDetailsModal.css";
import { API_ENDPOINTS } from "../utils/api";
import { toast } from "react-toastify";

const TOKEN_STORAGE_KEY = "jwt_token";

interface ServiceDetails {
  id: string;
  userId: string;
  title: string;
  price: string;
  priceMin: number;
  priceMax: number;
  description: string;
  location: string;
  tags: string[];
}

interface ServiceDetailsModalProps {
  service: ServiceDetails;
  onClose: () => void;
}

function PinIcon() {
  return (
    <svg viewBox="0 0 24 24" focusable="false" aria-hidden="true">
      <path d="M12 21s7-6.1 7-12A7 7 0 0 0 5 9c0 5.9 7 12 7 12Z" />
      <circle cx="12" cy="9" r="2.4" />
    </svg>
  );
}

function MessageIcon() {
  return (
    <span className="service-details-message-icon" aria-hidden="true">
      <svg viewBox="0 0 24 24" focusable="false">
        <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.8 8.8 0 0 1-3.7-.8L3 21l1.8-5a8.3 8.3 0 0 1-1-4.1 8.4 8.4 0 0 1 8.7-8.1A8.4 8.4 0 0 1 21 11.5Z" />
      </svg>
    </span>
  );
}

type ModalView = "details" | "booking" | "payment" | "success";

interface BookingFormState {
  agreedPrice: string;
  scheduledAt: string;
  error: string;
  isLoading: boolean;
}

function ServiceDetailsModal({
  service,
  onClose
}: ServiceDetailsModalProps) {
  const [view, setView] = useState<ModalView>("details");
  const [clientSecret, setClientSecret] = useState("");
  const currentUserId = localStorage.getItem("user_id");
  const [showReportDialog, setShowReportDialog] = useState(false);
  const [reportReason, setReportReason] = useState("Inappropriate content");
  const [isReporting, setIsReporting] = useState(false);
  const [form, setForm] = useState<BookingFormState>({
    agreedPrice: String(service.priceMin),
    scheduledAt: "",
    error: "",
    isLoading: false
  });

  async function handleBookingSubmit() {
    const price = Number(form.agreedPrice);

    if (!form.scheduledAt) {
      setForm(f => ({ ...f, error: "Please select a date and time." }));
      return;
    }

    if (!Number.isFinite(price) || price < service.priceMin || price > service.priceMax) {
      setForm(f => ({
        ...f,
        error: `Price must be between $${service.priceMin} and $${service.priceMax}.`
      }));
      return;
    }

    setForm(f => ({ ...f, isLoading: true, error: "" }));

    try {
      const authToken = localStorage.getItem(TOKEN_STORAGE_KEY);
      const response = await fetch(API_ENDPOINTS.bookings.create, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${authToken}`
        },
        body: JSON.stringify({
          serviceId: service.id,
          agreedPrice: price,
          scheduledAt: new Date(form.scheduledAt).toISOString()
        })
      });

      if (!response.ok) {
        throw new Error("Failed to create booking.");
      }

      const data = await response.json();
      setClientSecret(data.clientSecret);
      setView("payment");
    } catch (err) {
      setForm(f => ({
        ...f,
        error: err instanceof Error ? err.message : "Something went wrong."
      }));
    } finally {
      setForm(f => ({ ...f, isLoading: false }));
    }
  }

  async function handleReportSubmit() {
    setIsReporting(true);
    try {
      const token = localStorage.getItem("jwt_token");
      const response = await fetch(API_ENDPOINTS.reports.create, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          listingId: service.id,
          providerId: service.userId,
          reason: reportReason
        })
      });

      if (response.ok) {
        toast.success("Report submitted successfully");
        setShowReportDialog(false);
      } else {
        toast.error("Failed to submit report");
      }
    } catch {
      toast.error("Failed to submit report");
    } finally {
      setIsReporting(false);
    }
  }

  return (
    <div
      className="service-details-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}>
      <section
        className="service-details-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="service-details-title">

        <div className="service-details-header">
          <h2 id="service-details-title">
            {view === "details"
              ? service.title
              : view === "booking"
              ? "Book Service"
              : view === "payment"
              ? "Payment"
              : "Booking Confirmed"}
          </h2>
          <button
            type="button"
            className="service-details-close"
            aria-label="Close service details"
            onClick={onClose}>
            {"\u00d7"}
          </button>
        </div>

        {view === "details" && (
          <>
            <div className="service-details-location">
              <PinIcon />
              <span>{service.location}</span>
            </div>

            <div className="service-details-price">{service.price}</div>

            <section className="service-details-section">
              <h3>Description</h3>
              <p>{service.description}</p>
            </section>

            {service.tags.length > 0 && (
              <section className="service-details-section">
                <h3>Tags</h3>
                <div className="service-details-tags">
                  {service.tags.map((tag) => (
                    <span key={tag}>{tag}</span>
                  ))}
                </div>
              </section>
            )}

            <div className="service-details-actions">
              <button
                type="button"
                className="service-details-book"
                onClick={() => setView("booking")}>
                Book Now
              </button>
              <button type="button" className="service-details-message">
                <MessageIcon />
                Message
              </button>
            </div>

            {currentUserId !== service.userId && (
              <>
                <button
                  type="button"
                  className="service-details-report-link"
                  onClick={() => setShowReportDialog(true)}>
                  Report this listing
                </button>

                {showReportDialog && (
                  <div className="report-dialog">
                    <h4>Report this listing</h4>
                    <select
                      value={reportReason}
                      onChange={e => setReportReason(e.target.value)}>
                      <option>Inappropriate content</option>
                      <option>Spam or misleading</option>
                      <option>Fraudulent service</option>
                      <option>Harassment</option>
                      <option>Other</option>
                    </select>
                    <div className="report-dialog-actions">
                      <button
                        type="button"
                        onClick={handleReportSubmit}
                        disabled={isReporting}>
                        {isReporting ? "Submitting..." : "Submit Report"}
                      </button>
                      <button
                        type="button"
                        onClick={() => setShowReportDialog(false)}>
                        Cancel
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </>
        )}

        {view === "booking" && (
          <div className="service-details-booking-form">
            <p className="service-details-price">{service.price}</p>

            <InputField
              label="Date and time"
              type="datetime-local"
              value={form.scheduledAt}
              placeHolder=""
              onChange={e => setForm(f => ({ ...f, scheduledAt: e.target.value }))}
            />

            <InputField
              label={`Your price ($${service.priceMin} - $${service.priceMax})`}
              type="number"
              value={form.agreedPrice}
              placeHolder={String(service.priceMin)}
              onChange={e => setForm(f => ({ ...f, agreedPrice: e.target.value }))}
            />

            {form.error && (
              <p className="booking-error">{form.error}</p>
            )}

            <div className="service-details-actions">
              <SubmitButton
                label={form.isLoading ? "Processing..." : "Continue to Payment"}
                onClick={handleBookingSubmit}
              />
              <button
                type="button"
                className="service-details-message"
                onClick={() => setView("details")}>
                Back
              </button>
            </div>
          </div>
        )}

        {view === "payment" && (
          <div className="service-details-payment">
            <PaymentForm
              clientSecret={clientSecret}
              onSuccess={() => setView("success")}
              onError={(message) => setForm(f => ({ ...f, error: message }))}
            />
          </div>
        )}

        {view === "success" && (
          <div className="service-details-payment">
            <p>Your booking is confirmed!</p>
            <button
              type="button"
              className="service-details-book"
              onClick={onClose}>
              Done
            </button>
          </div>
        )}

      </section>
    </div>
  );
}

export default ServiceDetailsModal;
export type { ServiceDetails };
