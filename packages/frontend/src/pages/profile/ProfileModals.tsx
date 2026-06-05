import type { FormEvent } from "react";
import { REVIEW_MAX_LENGTH } from "./constants";
import type {
  CustomerBooking,
  ReviewDraft,
  ServiceListing
} from "./types";

interface DeleteServiceModalProps {
  isDeleting: boolean;
  service: ServiceListing;
  onCancel: () => void;
  onConfirm: (service: ServiceListing) => void;
}

export function DeleteServiceModal({
  isDeleting,
  service,
  onCancel,
  onConfirm
}: DeleteServiceModalProps) {
  return (
    <div
      className="profile-modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (
          event.target === event.currentTarget &&
          !isDeleting
        ) {
          onCancel();
        }
      }}>
      <section
        className="profile-confirm-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="take-down-title">
        <h2 id="take-down-title">Take Down Service</h2>
        <p>
          This will remove{" "}
          <strong>{service.title || "this service"}</strong>{" "}
          from your profile and campus services.
        </p>
        <div className="profile-confirm-actions">
          <button
            type="button"
            className="profile-confirm-danger"
            disabled={isDeleting}
            onClick={() => onConfirm(service)}>
            {isDeleting ? "Taking down..." : "Take Down"}
          </button>
          <button
            type="button"
            className="profile-confirm-cancel"
            disabled={isDeleting}
            onClick={onCancel}>
            Cancel
          </button>
        </div>
      </section>
    </div>
  );
}

interface ReviewModalProps {
  bookings: CustomerBooking[];
  reviewingBookingId: string;
  reviewDrafts: Record<string, ReviewDraft>;
  submittingReviewId: string | null;
  onClose: () => void;
  onSubmit: (
    event: FormEvent<HTMLFormElement>,
    booking: CustomerBooking
  ) => Promise<boolean>;
  onUpdateDraft: (
    bookingId: string,
    nextDraft: Partial<ReviewDraft>
  ) => void;
}

export function ReviewModal({
  bookings,
  reviewingBookingId,
  reviewDrafts,
  submittingReviewId,
  onClose,
  onSubmit,
  onUpdateDraft
}: ReviewModalProps) {
  const booking = bookings.find(
    (currentBooking) => currentBooking.id === reviewingBookingId
  );

  return (
    <div
      className="profile-modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}>
      <section
        className="profile-confirm-modal review-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-title">
        <h2 id="review-title">Leave a Review</h2>
        {booking && (
          <form
            className="review-form"
            onSubmit={(event) => {
              void onSubmit(event, booking).then(
                (wasSubmitted) => {
                  if (wasSubmitted) {
                    onClose();
                  }
                }
              );
            }}>
            <label>
              <span>Rating</span>
              <select
                value={
                  reviewDrafts[reviewingBookingId]?.rating ??
                  "5"
                }
                onChange={(event) =>
                  onUpdateDraft(reviewingBookingId, {
                    rating: event.target.value
                  })
                }>
                <option value="5">5</option>
                <option value="4">4</option>
                <option value="3">3</option>
                <option value="2">2</option>
                <option value="1">1</option>
              </select>
            </label>

            <label className="review-textarea-label">
              <span>Written review</span>
              <textarea
                value={
                  reviewDrafts[reviewingBookingId]?.review ?? ""
                }
                onChange={(event) =>
                  onUpdateDraft(reviewingBookingId, {
                    review: event.target.value
                  })
                }
                maxLength={REVIEW_MAX_LENGTH}
                rows={4}
                required
              />
            </label>

            <div className="profile-confirm-actions">
              <button
                type="submit"
                disabled={
                  submittingReviewId === reviewingBookingId
                }>
                {submittingReviewId === reviewingBookingId
                  ? "Submitting..."
                  : "Submit Review"}
              </button>
              <button
                type="button"
                className="profile-confirm-cancel"
                onClick={onClose}>
                Cancel
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
