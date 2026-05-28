import "./styles/ServiceBooking.css";
import type { ApiBooking, ApiUserProfile } from "../utils/types";
import { Badge, Card } from "react-bootstrap";
import { toast } from "react-toastify";
import { API_ENDPOINTS } from "../utils/api";
import { formatPriceUnit } from "../utils/pricing";
import { useEffect, useState } from "react";

interface ServiceBookingProps {
  booking: ApiBooking;
  user?: ApiUserProfile;
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

function ServiceBooking({ booking }: ServiceBookingProps) {
  const [customer, setCustomer] = useState<ApiUserProfile>();
  const [isUpdating, setIsUpdating] = useState(false);

  const authToken = window.localStorage.getItem("jwt_token");
  const customerName = customer
    ? `${customer.firstName} ${customer.lastName}`
    : "Customer";

  useEffect(() => {
    async function getBookingUser() {
      try {
        const response = await fetch(
          API_ENDPOINTS.user.other(booking.customerId),
          {
            headers: { Authorization: `Bearer ${authToken}` }
          }
        );

        if (response.ok) {
          const userData = (await response.json()) as ApiUserProfile;

          setCustomer(userData);
        } else if (response.status === 404) {
          toast.error("Could not fetch the customer");
        } else if (response.status === 401) {
          toast.error("Please log in");
        } else {
          toast.warning("Something went wrong");
        }
      } catch (e) {
        toast.warning("A network error occurred");
        console.error(e);
      }
    }

    getBookingUser();
  }, [authToken, booking.customerId]);

  async function confirmBooking() {
    setIsUpdating(true);

    try {
      const confirmResponse = await fetch(
        API_ENDPOINTS.bookings.confirm(booking.id),
        {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ confirmedPrice: booking.agreedPrice })
        }
      );

      if (confirmResponse.ok) {
        toast.success("Booking accepted");
      } else {
        toast.error("Something went wrong");
      }
    } catch (e) {
      console.error(e);
      toast.warning(
        "A network error occurred when confirming this booking, please try again."
      );
    } finally {
      setIsUpdating(false);
    }
  }

  async function cancelBooking() {
    setIsUpdating(true);

    try {
      const cancelResponse = await fetch(
        API_ENDPOINTS.bookings.cancel(booking.id),
        {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          }
        }
      );

      if (cancelResponse.ok) {
        toast.success("Booking canceled");
      } else {
        toast.error("Something went wrong");
      }
    } catch (e) {
      toast.warning("A network error occurred, please try again");
      console.error(e);
    } finally {
      setIsUpdating(false);
    }
  }

  return (
    <Card className="booking-card">
      <Card.Body className="booking-card-body">
        <div className="booking-card-header">
          <div>
            <p className="booking-card-eyebrow">Booking</p>
            <Card.Title className="booking-card-title">
              {booking.serviceTitle}
            </Card.Title>
          </div>
          <Badge bg="none" className="booking-status">
            {getStatusLabel(booking.status)}
          </Badge>
        </div>

        <div className="booking-details">
          <div>
            <span className="booking-detail-label">Customer</span>
            <span className="booking-detail-value">{customerName}</span>
          </div>
          <div>
            <span className="booking-detail-label">Scheduled</span>
            <span className="booking-detail-value">
              {formatBookingTime(booking.scheduledAt)}
            </span>
          </div>
        </div>

        <div className="booking-card-footer">
          <span className="booking-price">
            {formatBookingPrice(booking)}
          </span>

          {booking.status === "AWAITING_PROVIDER_CONFIRMATION" && (
            <button
              type="button"
              className="booking-action booking-action-primary"
              disabled={isUpdating}
              onClick={confirmBooking}>
              {isUpdating ? "Accepting..." : "Accept Booking"}
            </button>
          )}

          {booking.status === "CONFIRMED" && (
            <button
              type="button"
              className="booking-action booking-action-secondary"
              disabled={isUpdating}
              onClick={cancelBooking}>
              {isUpdating ? "Canceling..." : "Cancel Booking"}
            </button>
          )}
        </div>
      </Card.Body>
    </Card>
  );
}

export default ServiceBooking;
