import { useEffect, useState } from "react";
import { API_ENDPOINTS } from "../utils/api";
import { formatDateTime } from "../pages/profile/utils";
import "./styles/ReviewsModal.css";

interface Review {
  id: string;
  serviceTitle: string;
  rating: number;
  review: string;
  reviewerName: string;
  reviewedAt: string;
}

interface ReviewsModalProps {
  providerId: string;
  providerName: string;
  onClose: () => void;
}

export function ReviewsModal({
  providerId,
  providerName,
  onClose
}: ReviewsModalProps) {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function fetchReviews() {
      try {
        const token = localStorage.getItem("jwt_token");
        const response = await fetch(
          API_ENDPOINTS.bookings.providerReviews(providerId),
          {
            headers: {
              Authorization: `Bearer ${token}`
            }
          }
        );

        if (!response.ok) {
          throw new Error("Failed to load reviews");
        }

        const data = await response.json();
        setReviews(
          data.map(
            (booking: {
              id: string;
              serviceTitle: string;
              rating: number;
              review: string;
              reviewerName: string;
              reviewedAt: string;
            }) => ({
              id: booking.id,
              serviceTitle: booking.serviceTitle,
              rating: booking.rating,
              review: booking.review,
              reviewerName: booking.reviewerName,
              reviewedAt: booking.reviewedAt
            })
          )
        );
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "Failed to load reviews"
        );
      } finally {
        setLoading(false);
      }
    }

    fetchReviews();
  }, [providerId]);

  return (
    <div
      className="reviews-modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}>
      <div
        className="reviews-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reviews-modal-title">
        <div className="reviews-modal-header">
          <h2 id="reviews-modal-title">
            Reviews for {providerName}
          </h2>
          <button
            type="button"
            className="reviews-modal-close"
            aria-label="Close reviews"
            onClick={onClose}>
            {"\u00d7"}
          </button>
        </div>

        <div className="reviews-modal-content">
          {loading ? (
            <p className="reviews-empty">Loading reviews...</p>
          ) : error ? (
            <p className="reviews-error">{error}</p>
          ) : reviews.length === 0 ? (
            <p className="reviews-empty">No reviews yet.</p>
          ) : (
            <div className="reviews-list">
              {reviews.map((review) => (
                <article
                  key={review.id}
                  className="review-item">
                  <div className="review-header">
                    <div>
                      <h3>{review.serviceTitle}</h3>
                      <p className="review-meta">
                        By {review.reviewerName}
                      </p>
                    </div>
                    <div className="review-rating">
                      <span aria-hidden="true">{"\u2605"}</span>
                      <strong>{review.rating}/5</strong>
                    </div>
                  </div>
                  <p className="review-text">{review.review}</p>
                  <p className="review-date">
                    {formatDateTime(review.reviewedAt)}
                  </p>
                </article>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
