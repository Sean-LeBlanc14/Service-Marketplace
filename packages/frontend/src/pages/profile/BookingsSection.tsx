import { REVIEWABLE_BOOKING_STATUS } from "./constants";
import type { CustomerBooking } from "./types";
import {
  formatBookingPrice,
  formatBookingStatus,
  formatBookingStatusClass,
  formatDateTime
} from "./utils";

interface BookingsSectionProps {
  bookings: CustomerBooking[];
  isLoading: boolean;
  onReviewBooking: (bookingId: string) => void;
}

export function BookingsSection({
  bookings,
  isLoading,
  onReviewBooking
}: BookingsSectionProps) {
  return (
    <section
      className="profile-section bookings-section"
      aria-label="Bookings">
      <div className="section-heading">
        <div>
          <h2>Bookings</h2>
          <p>Services you booked as a customer.</p>
        </div>
      </div>

      {isLoading ? (
        <p className="empty-state">Loading bookings...</p>
      ) : bookings.length === 0 ? (
        <p className="empty-state">
          You have not booked any services yet.
        </p>
      ) : (
        <div className="booking-grid">
          {bookings.map((booking) => {
            const canReview =
              booking.status === REVIEWABLE_BOOKING_STATUS;
            const hasReview =
              booking.rating !== null &&
              booking.review.length > 0;

            return (
              <article
                className="booking-card"
                key={booking.id}>
                <div className="booking-card-heading">
                  <div>
                    <h3>{booking.serviceTitle}</h3>
                    <p
                      className={`booking-status booking-status--${formatBookingStatusClass(booking.status)}`}>
                      {formatBookingStatus(booking.status)}
                    </p>
                  </div>
                  <strong>{formatBookingPrice(booking)}</strong>
                </div>

                <p className="booking-scheduled">
                  Scheduled{" "}
                  {formatDateTime(booking.scheduledAt)}
                </p>
                {booking.providerName && (
                  <p className="booking-user">
                    Provider {booking.providerName}
                  </p>
                )}

                {canReview && !hasReview && (
                  <button
                    type="button"
                    className="add-review-button"
                    onClick={() => onReviewBooking(booking.id)}>
                    Add Review
                  </button>
                )}
                {hasReview && (
                  <div className="submitted-review">
                    <p>
                      <strong>
                        Rating by {booking.reviewerName}:
                      </strong>{" "}
                      {booking.rating}/5
                    </p>
                    <p>{booking.review}</p>
                    {booking.reviewedAt && (
                      <p className="reviewed-at">
                        Reviewed{" "}
                        {formatDateTime(booking.reviewedAt)}
                      </p>
                    )}
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
