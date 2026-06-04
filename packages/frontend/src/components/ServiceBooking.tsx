import "./styles/ServiceBooking.css";
import type { ApiBooking } from "../utils/types";
import { Badge, Card } from "react-bootstrap";
import { formatPriceUnit } from "../utils/pricing";
import { useState } from "react";

interface ServiceBookingProps {
  booking: ApiBooking;
  confirmBooking?: (
    booking: ApiBooking
  ) => Promise<boolean> | boolean;
  rejectBooking?: (
    booking: ApiBooking
  ) => Promise<boolean> | boolean;
  cancelBooking?: (
    booking: ApiBooking
  ) => Promise<boolean> | boolean;
}

function formatBookingTime(
  isoString: string,
  locale: string = "en-US"
): string {
  const date = new Date(isoString);

  if (Number.isNaN(date.getTime())) {
    return "Invalid date";
  }

  return new Intl.DateTimeFormat(locale, {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
    hour12: true
  }).format(date);
}

function formatBookingPrice(booking: ApiBooking): string {
  const unit = formatPriceUnit(booking.priceUnit);
  const unitLabel = unit ? `/${unit.toLowerCase()}` : "";

  return `$${booking.agreedPrice}${unitLabel}`;
}

function getStatusLabel(status: string): string {
  return status
    .split("_")
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(" ");
}

function ServiceBooking({
  booking,
  confirmBooking,
  cancelBooking,
  rejectBooking
}: ServiceBookingProps) {
  const [isUpdating, setIsUpdating] = useState(false);
  const [isRejecting, setIsRejecting] = useState(false);
  const hasBookingActions =
    booking.status === "AWAITING_PROVIDER_CONFIRMATION" ||
    booking.status === "CONFIRMED";

  return (
    <Card
      className={`booking-card ${
        hasBookingActions ? "" : "booking-card-compact"
      }`}>
      <Card.Body className="booking-card-body">
        <div className="booking-card-header">
          <div>
            <p className="booking-card-eyebrow">Booking</p>
            <Card.Title className="booking-card-title">
              {booking.serviceTitle}
            </Card.Title>
          </div>
          <Badge bg="none" className="booking-status">
            {getStatusLabel(booking.status as string)}
          </Badge>
        </div>

        <div className="booking-details">
          <div>
            <span className="booking-detail-label">
              Customer
            </span>
            <span className="booking-detail-value">
              {booking.customerName}
            </span>
          </div>
          <div>
            <span className="booking-detail-label">
              Scheduled
            </span>
            <span className="booking-detail-value">
              {formatBookingTime(booking.scheduledAt as string)}
            </span>
          </div>
        </div>

        <div
          className={`booking-card-footer ${
            hasBookingActions
              ? ""
              : "booking-card-footer-compact"
          }`}>
          <span className="booking-price">
            {formatBookingPrice(booking)}
          </span>

          {booking.status ===
            "AWAITING_PROVIDER_CONFIRMATION" && (
            <div className="booking-action-container">
              <button
                type="button"
                className="booking-action booking-action-primary"
                disabled={isUpdating}
                onClick={async () => {
                  setIsUpdating(true);
                  const succeeded =
                    (await confirmBooking?.(booking)) ?? false;

                  if (!succeeded) {
                    setIsUpdating(false);
                  }
                }}>
                {isUpdating ? "Accepting..." : "Accept Booking"}
              </button>

              <button
                type="button"
                className="booking-action booking-action-secondary"
                disabled={isRejecting}
                onClick={async () => {
                  setIsRejecting(true);
                  const succeeded =
                    (await rejectBooking?.(booking)) ?? false;

                  if (!succeeded) {
                    setIsRejecting(false);
                  }
                }}>
                {isRejecting
                  ? "Rejecting..."
                  : "Reject Booking"}
              </button>
            </div>
          )}

          {booking.status === "CONFIRMED" && (
            <button
              type="button"
              className="booking-action booking-action-primary"
              disabled={isUpdating}
              onClick={async () => {
                setIsUpdating(true);
                const succeeded =
                  (await cancelBooking?.(booking)) ?? false;

                if (!succeeded) {
                  setIsUpdating(false);
                }
              }}>
              {isUpdating ? "Canceling..." : "Cancel Booking"}
            </button>
          )}
        </div>
      </Card.Body>
    </Card>
  );
}

export default ServiceBooking;
