import { API_ENDPOINTS } from "../utils/api";
import type { ApiBooking } from "../utils/types";
import ServiceBooking from "../components/ServiceBooking";
import DropDown from "../components/DropDown";
import Modal from "../components/Modal";
import { toast } from "react-toastify";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/ServiceDashboard.css";
import { getToken } from "../utils/helper";

type DashboardView =
  | "requests"
  | "upcoming"
  | "completed"
  | "customerRequests"
  | "customerScheduled"
  | "customerCompleted";

const REVIEW_MAX_LENGTH = 1000;

export default function ServiceDashboard() {
  const navigate = useNavigate();

  const [bookingRequests, setBookingRequests] = useState<
    ApiBooking[]
  >([]);

  const [serviceHistory, setServiceHistory] = useState<
    ApiBooking[]
  >([]);

  const [upcomingBookings, setUpcomingBookings] = useState<
    ApiBooking[]
  >([]);

  const [customerBookings, setCustomerBookings] = useState<
    ApiBooking[]
  >([]);

  const [selectedView, setSelectedView] =
    useState<DashboardView>("requests");
  const [reviewingBooking, setReviewingBooking] =
    useState<ApiBooking | null>(null);
  const [reviewRating, setReviewRating] = useState("5");
  const [reviewText, setReviewText] = useState("");
  const [submittingReviewId, setSubmittingReviewId] = useState<
    string | null
  >(null);

  const authToken = getToken();

  useEffect(() => {
    if (!authToken) {
      navigate("/login");
      toast.error("Please login");
      return;
    }

    //Will pull up all bookings
    const fetchUserServices = async () => {
      try {
        //Pulls all the requested bookings aka bookings with status AWAITING_PROVIDER_CONFIRMATION
        const bookingRequestResponse = await fetch(
          API_ENDPOINTS.bookings.getProviderRequests,
          {
            headers: {
              Authorization: `Bearer ${authToken}`,
              Accept: "application/json"
            }
          }
        );

        if (bookingRequestResponse.ok) {
          const bookings =
            (await bookingRequestResponse.json()) as ApiBooking[];

          setBookingRequests(bookings);
        } else {
          toast.warning("Something went wrong");
        }

        //Completed bookings with status completed
        const serviceHistoryResponse = await fetch(
          API_ENDPOINTS.bookings.getProviderCompleted,
          {
            headers: {
              Authorization: `Bearer ${authToken}`,
              Accept: "application/json"
            }
          }
        );

        if (serviceHistoryResponse.ok) {
          const serviceHistory =
            (await serviceHistoryResponse.json()) as ApiBooking[];

          setServiceHistory(serviceHistory);
        } else {
          toast.warning("Something went wrong");
        }

        //Scheduled bookings with status CONFIRMED
        const scheduledBookingsResponse = await fetch(
          API_ENDPOINTS.bookings.getProviderScheduled,
          {
            headers: {
              Authorization: `Bearer ${authToken}`,
              Accept: "application/json"
            }
          }
        );

        if (scheduledBookingsResponse.ok) {
          const scheduledBookings =
            (await scheduledBookingsResponse.json()) as ApiBooking[];

          setUpcomingBookings(scheduledBookings);
        } else {
          toast.warning("A network error occurred");
        }

        const customerBookingsResponse = await fetch(
          API_ENDPOINTS.bookings.mine,
          {
            headers: {
              Authorization: `Bearer ${authToken}`,
              Accept: "application/json"
            }
          }
        );

        if (customerBookingsResponse.ok) {
          const customerBookings =
            (await customerBookingsResponse.json()) as ApiBooking[];

          setCustomerBookings(customerBookings);
        } else {
          toast.warning("Could not get your customer bookings");
        }
      } catch (e) {
        console.error(e);
        toast.warning(
          "A network error occurred, please reload the page."
        );
      }
    };
    fetchUserServices();
  }, [
    navigate,
    setBookingRequests,
    setServiceHistory,
    setUpcomingBookings,
    setCustomerBookings,
    authToken
  ]);

  async function confirmBooking(
    booking: ApiBooking
  ): Promise<boolean> {
    if (!authToken) {
      navigate("/login");
      toast.error("Please login");
      return false;
    }

    try {
      const confirmResponse = await fetch(
        API_ENDPOINTS.bookings.confirm(booking.id as string),
        {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            confirmedPrice: booking.agreedPrice
          })
        }
      );

      if (confirmResponse.ok) {
        toast.success(
          "Booking accepted, you will receive payment shortly!"
        );
        setBookingRequests(
          bookingRequests?.filter(
            (request) => booking.id !== request.id
          )
        );
        return true;
      } else {
        toast.error("Something went wrong");
      }
    } catch {
      toast.warning(
        "A network error occurred, please try again"
      );
    }

    return false;
  }

  async function rejectBooking(
    booking: ApiBooking
  ): Promise<boolean> {
    if (!authToken) {
      navigate("/login");
      toast.error("Please login");
      return false;
    }

    try {
      const rejectionResponse = await fetch(
        API_ENDPOINTS.bookings.reject(booking.id as string),
        {
          method: "DELETE",
          headers: { Authorization: `Bearer ${authToken}` }
        }
      );

      if (rejectionResponse.ok) {
        toast.success("Booking successfully rejected.");
        setBookingRequests(
          bookingRequests?.filter(
            (request) => request.id !== booking.id
          )
        );
        return true;
      } else {
        toast.error("Could not reject this booking.");
      }
    } catch {
      toast.warning(
        "A network error occurred, please try again"
      );
    }

    return false;
  }

  async function cancelBooking(
    booking: ApiBooking
  ): Promise<boolean> {
    if (!authToken) {
      navigate("/login");
      toast.error("Please login");
      return false;
    }

    try {
      const cancelResponse = await fetch(
        API_ENDPOINTS.bookings.cancel(booking.id as string),
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
        setUpcomingBookings(
          upcomingBookings?.filter(
            (appointment) => appointment.id !== booking.id
          )
        );
        setCustomerBookings((bookings) =>
          bookings.filter(
            (appointment) => appointment.id !== booking.id
          )
        );
        return true;
      } else {
        toast.error("Something went wrong");
      }
    } catch (e) {
      toast.warning(
        "A network error occurred, please try again"
      );
      console.error(e);
    }

    return false;
  }

  function openReviewModal(booking: ApiBooking) {
    setReviewingBooking(booking);
    setReviewRating("5");
    setReviewText("");
  }

  async function submitReview() {
    if (!reviewingBooking?.id) {
      toast.error("Could not submit review.");
      return;
    }

    if (!authToken) {
      navigate("/login");
      toast.error("Please login");
      return;
    }

    const rating = Number(reviewRating);
    const review = reviewText.trim();

    if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
      toast.error("Choose a rating from 1 to 5.");
      return;
    }

    if (!review) {
      toast.error("Write a review before submitting.");
      return;
    }

    if (review.length > REVIEW_MAX_LENGTH) {
      toast.error("Keep the review to 1000 characters or fewer.");
      return;
    }

    setSubmittingReviewId(reviewingBooking.id);

    try {
      const response = await fetch(
        API_ENDPOINTS.bookings.review(reviewingBooking.id),
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ rating, review })
        }
      );

      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Could not submit review.");
      }

      const reviewedBooking = (await response.json()) as ApiBooking;

      setCustomerBookings((bookings) =>
        bookings.map((booking) =>
          booking.id === reviewedBooking.id ? reviewedBooking : booking
        )
      );
      toast.success("Review submitted");
      setReviewingBooking(null);
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "Could not submit review."
      );
    } finally {
      setSubmittingReviewId(null);
    }
  }

  const customerRequestBookings = customerBookings.filter(
    (booking) => booking.status === "AWAITING_PROVIDER_CONFIRMATION"
  );
  const customerScheduledBookings = customerBookings.filter(
    (booking) => booking.status === "CONFIRMED"
  );
  const customerCompletedBookings = customerBookings.filter(
    (booking) => booking.status === "COMPLETED"
  );

  const dashboardViews: Record<
    DashboardView,
    {
      heading: string;
      emptyMessage: string;
      bookings: ApiBooking[] | undefined;
      viewAs: "provider" | "customer";
    }
  > = {
    requests: {
      heading: "Booking Requests as a Provider",
      emptyMessage: "No incoming requests",
      bookings: bookingRequests,
      viewAs: "provider"
    },
    upcoming: {
      heading: "Scheduled Bookings as a Provider",
      emptyMessage: "No upcoming bookings",
      bookings: upcomingBookings,
      viewAs: "provider"
    },
    completed: {
      heading: "Completed Bookings as a Provider",
      emptyMessage: "No completed bookings",
      bookings: serviceHistory,
      viewAs: "provider"
    },
    customerRequests: {
      heading: "Requested Bookings as a Customer",
      emptyMessage: "No requested customer bookings",
      bookings: customerRequestBookings,
      viewAs: "customer"
    },
    customerScheduled: {
      heading: "Scheduled Bookings as a Customer",
      emptyMessage: "No scheduled customer bookings",
      bookings: customerScheduledBookings,
      viewAs: "customer"
    },
    customerCompleted: {
      heading: "Completed Bookings as a Customer",
      emptyMessage: "No completed customer bookings",
      bookings: customerCompletedBookings,
      viewAs: "customer"
    }
  };

  const activeView = dashboardViews[selectedView];

  function renderBooking(booking: ApiBooking) {
    const participantProps =
      activeView.viewAs === "customer"
        ? {
            participantLabel: "Provider",
            participantName: booking.providerName
          }
        : {
            participantLabel: "Customer",
            participantName: booking.customerName
          };

    if (selectedView === "requests") {
      return (
        <ServiceBooking
          key={booking.id}
          booking={booking}
          {...participantProps}
          confirmBooking={confirmBooking}
          rejectBooking={rejectBooking}
        />
      );
    }

    if (selectedView === "upcoming") {
      return (
        <ServiceBooking
          key={booking.id}
          booking={booking}
          {...participantProps}
          cancelBooking={cancelBooking}
        />
      );
    }

    if (
      activeView.viewAs === "customer" &&
      booking.status === "CONFIRMED"
    ) {
      return (
        <ServiceBooking
          key={booking.id}
          booking={booking}
          {...participantProps}
          cancelBooking={cancelBooking}
        />
      );
    }

    if (
      activeView.viewAs === "customer" &&
      booking.status === "COMPLETED"
    ) {
      return (
        <ServiceBooking
          key={booking.id}
          booking={booking}
          {...participantProps}
          onReviewBooking={openReviewModal}
        />
      );
    }

    return (
      <ServiceBooking
        key={booking.id}
        booking={booking}
        {...participantProps}
      />
    );
  }

  return (
    <div className="serviceDashboard-wrapper">
      <h1>Your Bookings Dashboard</h1>

      <div className="service-dashboard-filter">
        <DropDown
          label="View"
          value={selectedView}
          placeHolder="Select bookings"
          onChange={(event) =>
            setSelectedView(event.target.value as DashboardView)
          }
          options={
            <>
              <optgroup label="As provider">
                <option value="requests">Requests</option>
                <option value="upcoming">
                  Scheduled Bookings
                </option>
                <option value="completed">
                  Completed Bookings
                </option>
              </optgroup>
              <optgroup label="As customer">
                <option value="customerRequests">Requests</option>
                <option value="customerScheduled">
                  Scheduled Bookings
                </option>
                <option value="customerCompleted">
                  Completed Bookings
                </option>
              </optgroup>
            </>
          }
        />
      </div>

      <section>
        <h2>{activeView.heading}</h2>
        <div className="booking-container">
          {activeView.bookings && activeView.bookings.length > 0
            ? activeView.bookings.map((booking: ApiBooking) =>
                renderBooking(booking)
              )
            : activeView.emptyMessage}
        </div>
      </section>

      <Modal
        isOpen={reviewingBooking !== null}
        onClose={() => setReviewingBooking(null)}>
        {reviewingBooking && (
          <div className="dashboard-review-modal">
            <p className="dashboard-review-eyebrow">Completed Booking</p>
            <h2>Review {reviewingBooking.serviceTitle}</h2>
            {reviewingBooking.providerName && (
              <p className="dashboard-review-provider">
                Provider {reviewingBooking.providerName}
              </p>
            )}

            <form
              className="dashboard-review-form"
              onSubmit={(event) => {
                event.preventDefault();
                void submitReview();
              }}>
              <label>
                <span>Rating</span>
                <select
                  value={reviewRating}
                  onChange={(event) =>
                    setReviewRating(event.target.value)
                  }>
                  <option value="5">5 - Excellent</option>
                  <option value="4">4 - Good</option>
                  <option value="3">3 - Okay</option>
                  <option value="2">2 - Poor</option>
                  <option value="1">1 - Bad</option>
                </select>
              </label>

              <label>
                <span>Written review</span>
                <textarea
                  value={reviewText}
                  maxLength={REVIEW_MAX_LENGTH}
                  onChange={(event) =>
                    setReviewText(event.target.value)
                  }
                  rows={5}
                />
              </label>

              <button
                type="submit"
                disabled={submittingReviewId === reviewingBooking.id}>
                {submittingReviewId === reviewingBooking.id
                  ? "Submitting..."
                  : "Submit Review"}
              </button>
            </form>
          </div>
        )}
      </Modal>
    </div>
  );
}
