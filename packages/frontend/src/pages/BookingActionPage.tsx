import { useEffect, useState } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { API_ENDPOINTS } from "../utils/api";
import "../styles/BookingActionPage.css";

type Status =
  | "loading"
  | "success"
  | "invalid"
  | "already_actioned"
  | "error";

export default function BookingActionPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const [status, setStatus] = useState<Status>("loading");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!token) {
      setStatus("invalid");
      setMessage("No token provided.");
      return;
    }

    fetch(API_ENDPOINTS.bookings.action(token))
      .then(async (res) => {
        const data = await res.json();
        setStatus(data.status as Status);
        setMessage(data.message);
      })
      .catch(() => {
        setStatus("error");
        setMessage(
          "Unable to connect to the server. Please try again."
        );
      });
  }, [token]);

  const headingMap: Record<Status, string> = {
    loading: "Processing...",
    success: "Booking Updated",
    invalid: "Link Invalid",
    already_actioned: "Already Actioned",
    error: "Something Went Wrong"
  };

  return (
    <div className="booking-action-page">
      <div className="booking-action-card">
        <h1
          className={`booking-action-heading booking-action-heading--${status}`}>
          {headingMap[status]}
        </h1>
        {status !== "loading" && (
          <>
            <p className="booking-action-message">{message}</p>
            <Link to="/" className="booking-action-link">
              Go to Home
            </Link>
          </>
        )}
      </div>
    </div>
  );
}
