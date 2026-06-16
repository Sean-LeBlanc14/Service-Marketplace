import { useEffect, useRef, useState } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { API_ENDPOINTS } from "../utils/api";
import "./styles/BookingActionPage.css";

type Status = "loading" | "success" | "warning" | "error";

interface ActionResult {
  title: string;
  message: string;
  status: Status;
}

export default function BookingActionPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const [result, setResult] = useState<ActionResult | null>(
    token
      ? null
      : {
          status: "error",
          title: "Link Invalid",
          message: "No token was provided in this link."
        }
  );
  const hasFetched = useRef(false);

  useEffect(() => {
    if (!token || hasFetched.current) return;
    hasFetched.current = true;

    fetch(API_ENDPOINTS.bookings.action(token))
      .then(async (res) => {
        const data = (await res.json()) as ActionResult;
        setResult(data);
      })
      .catch(() => {
        setResult({
          status: "error",
          title: "Something Went Wrong",
          message:
            "Unable to process your request. Please try again later."
        });
      });
  }, [token]);

  return (
    <div className="booking-action-page">
      <div className="booking-action-card">
        {!result ? (
          <p className="booking-action-loading">
            Processing...
          </p>
        ) : (
          <>
            <h1
              className={`booking-action-title booking-action-title--${result.status}`}>
              {result.title}
            </h1>
            <p className="booking-action-message">
              {result.message}
            </p>
            <Link
              to="/dashboard"
              className="booking-action-link">
              Go to Dashboard
            </Link>
          </>
        )}
      </div>
    </div>
  );
}
