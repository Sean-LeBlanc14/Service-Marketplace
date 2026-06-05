import "../styles/Calendar.css";
import { useEffect, useMemo, useState } from "react";
import { getToken } from "../utils/helper";
import type { ApiBooking } from "../utils/types";
import { API_ENDPOINTS } from "../utils/api";
import { toast } from "react-toastify";
import DropDown from "../components/DropDown";
import Modal from "../components/Modal";
import { FaCircleInfo } from "react-icons/fa6";

type CalendarView = "provider" | "customer";

const WEEKDAYS = [
  "Sun",
  "Mon",
  "Tue",
  "Wed",
  "Thu",
  "Fri",
  "Sat"
];

function formatMonthHeading(date: Date) {
  return new Intl.DateTimeFormat(undefined, {
    month: "long",
    year: "numeric"
  }).format(date);
}

function getDateKey(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function getBookingDateKey(booking: ApiBooking) {
  if (!booking.scheduledAt) {
    return "";
  }

  const scheduledDate = new Date(booking.scheduledAt);

  if (Number.isNaN(scheduledDate.getTime())) {
    return "";
  }

  return getDateKey(scheduledDate);
}

function formatBookingTime(scheduledAt?: string | null) {
  if (!scheduledAt) {
    return "Time TBD";
  }

  const scheduledDate = new Date(scheduledAt);

  if (Number.isNaN(scheduledDate.getTime())) {
    return "Time TBD";
  }

  return new Intl.DateTimeFormat(undefined, {
    hour: "numeric",
    minute: "2-digit"
  }).format(scheduledDate);
}

function formatBookingDateTime(scheduledAt?: string | null) {
  if (!scheduledAt) {
    return "Time TBD";
  }

  const scheduledDate = new Date(scheduledAt);

  if (Number.isNaN(scheduledDate.getTime())) {
    return "Time TBD";
  }

  return new Intl.DateTimeFormat(undefined, {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit"
  }).format(scheduledDate);
}

function getMonthDays(monthDate: Date) {
  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const days: Array<Date | null> = [];

  for (let index = 0; index < firstDay.getDay(); index += 1) {
    days.push(null);
  }

  for (let day = 1; day <= lastDay.getDate(); day += 1) {
    days.push(new Date(year, month, day));
  }

  while (days.length % 7 !== 0) {
    days.push(null);
  }

  return days;
}

export default function Calendar() {
  const authToken = getToken();
  const [
    scheduledProviderBookings,
    setScheduledProviderBookings
  ] = useState<ApiBooking[]>([]);
  const [
    scheduledCustomerBookings,
    setScheduledCustomerBookings
  ] = useState<ApiBooking[]>([]);
  const [selectedView, setSelectedView] =
    useState<CalendarView>("provider");
  const [visibleMonth, setVisibleMonth] = useState(
    () => new Date()
  );
  const [selectedBooking, setSelectedBooking] =
    useState<ApiBooking | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  useEffect(() => {
    async function getUserBookings() {
      if (!authToken) {
        return;
      }

      try {
        const [providerResponse, customerResponse] =
          await Promise.all([
            fetch(API_ENDPOINTS.bookings.getProviderScheduled, {
              headers: { Authorization: `Bearer ${authToken}` }
            }),
            fetch(API_ENDPOINTS.bookings.getCustomerScheduled, {
              headers: { Authorization: `Bearer ${authToken}` }
            })
          ]);

        if (providerResponse.ok) {
          const bookings =
            (await providerResponse.json()) as ApiBooking[];
          setScheduledProviderBookings(bookings);
        } else {
          toast.error("Could not get your provided services");
        }

        if (customerResponse.ok) {
          const bookings =
            (await customerResponse.json()) as ApiBooking[];
          setScheduledCustomerBookings(bookings);
        } else {
          toast.error("Could not get your purchased services");
        }
      } catch {
        toast.warning("A network error occurred");
      }
    }

    void getUserBookings();
  }, [authToken]);

  const calendarViews: Record<
    CalendarView,
    { heading: string; bookings: ApiBooking[] }
  > = useMemo(
    () => ({
      provider: {
        heading: "Provider Calendar",
        bookings: scheduledProviderBookings
      },
      customer: {
        heading: "Customer Calendar",
        bookings: scheduledCustomerBookings
      }
    }),
    [scheduledProviderBookings, scheduledCustomerBookings]
  );

  const selectedCalendar = calendarViews[selectedView];
  const days = useMemo(
    () => getMonthDays(visibleMonth),
    [visibleMonth]
  );
  const bookingsByDay = useMemo(() => {
    return selectedCalendar.bookings.reduce<
      Record<string, ApiBooking[]>
    >((bookingsMap, booking) => {
      const dateKey = getBookingDateKey(booking);

      if (!dateKey) {
        return bookingsMap;
      }

      bookingsMap[dateKey] = [
        ...(bookingsMap[dateKey] ?? []),
        booking
      ];
      return bookingsMap;
    }, {});
  }, [selectedCalendar.bookings]);

  function goToPreviousMonth() {
    setVisibleMonth(
      (currentMonth) =>
        new Date(
          currentMonth.getFullYear(),
          currentMonth.getMonth() - 1,
          1
        )
    );
  }

  function goToNextMonth() {
    setVisibleMonth(
      (currentMonth) =>
        new Date(
          currentMonth.getFullYear(),
          currentMonth.getMonth() + 1,
          1
        )
    );
  }

  function getBookingPersonName(booking: ApiBooking) {
    if (selectedView === "customer") {
      return booking.providerName || "Provider";
    }

    return booking.customerName || "Customer";
  }

  function getOppositePersonLabel() {
    return selectedView === "customer"
      ? "Provider"
      : "Customer";
  }

  function formatBookingPrice(booking: ApiBooking) {
    if (!booking.agreedPrice) {
      return "Price TBD";
    }

    return booking.priceUnit
      ? `$${booking.agreedPrice}/${booking.priceUnit}`
      : `$${booking.agreedPrice}`;
  }

  async function cancelBooking() {
    if (!selectedBooking?.id) {
      toast.error("Could not cancel this booking.");
      return;
    }

    if (!authToken) {
      toast.error("Please login");
      return;
    }

    setIsCancelling(true);

    try {
      const response = await fetch(
        API_ENDPOINTS.bookings.cancel(selectedBooking.id),
        {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          }
        }
      );

      if (response.ok) {
        toast.success("Booking canceled");
        setScheduledProviderBookings((bookings) =>
          bookings.filter(
            (booking) => booking.id !== selectedBooking.id
          )
        );
        setScheduledCustomerBookings((bookings) =>
          bookings.filter(
            (booking) => booking.id !== selectedBooking.id
          )
        );
        setSelectedBooking(null);
      } else {
        toast.error("Something went wrong");
      }
    } catch {
      toast.warning(
        "A network error occurred, please try again"
      );
    } finally {
      setIsCancelling(false);
    }
  }

  async function completeBooking() {
    if (!selectedBooking?.id) {
      toast.error("Could not complete this booking.");
      return;
    }

    if (!authToken) {
      toast.error("Please login");
      return;
    }

    setIsCompleting(true);

    try {
      const response = await fetch(
        API_ENDPOINTS.bookings.complete(selectedBooking.id),
        {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          }
        }
      );

      if (response.ok) {
        toast.success("Booking marked complete");
        setScheduledProviderBookings((bookings) =>
          bookings.filter(
            (booking) => booking.id !== selectedBooking.id
          )
        );
        setSelectedBooking(null);
      } else {
        toast.error("Could not mark booking complete.");
      }
    } catch {
      toast.warning(
        "A network error occurred, please try again"
      );
    } finally {
      setIsCompleting(false);
    }
  }

  return (
    <div className="calendar-wrapper">
      <div className="calendar-header">
        <div>
          <h1>Your Schedule</h1>
          <p>{selectedCalendar.heading}</p>
        </div>

        <div className="calendar-view-control">
          <DropDown
            label="Calendar View"
            value={selectedView}
            onChange={(event) =>
              setSelectedView(
                event.target.value as CalendarView
              )
            }
            placeHolder="Select Calendar View"
            options={
              <>
                <option value="provider">
                  Provider Calendar
                </option>
                <option value="customer">
                  Customer Calendar
                </option>
              </>
            }
          />
        </div>
      </div>

      <div className="calendar-month-bar">
        <button type="button" onClick={goToPreviousMonth}>
          Previous
        </button>
        <h2>{formatMonthHeading(visibleMonth)}</h2>
        <button type="button" onClick={goToNextMonth}>
          Next
        </button>
      </div>

      <div className="calendar-grid">
        {WEEKDAYS.map((weekday) => (
          <div className="calendar-weekday" key={weekday}>
            {weekday}
          </div>
        ))}

        {days.map((day, index) => {
          const dateKey = day
            ? getDateKey(day)
            : `blank-${index}`;
          const dayBookings = day
            ? (bookingsByDay[dateKey] ?? [])
            : [];

          return (
            <div
              className={`calendar-day ${!day ? "is-empty" : ""}`}
              key={dateKey}>
              {day && (
                <>
                  <span className="calendar-day-number">
                    {day.getDate()}
                  </span>

                  <div className="calendar-bookings">
                    {dayBookings.map((booking) => (
                      <button
                        type="button"
                        className="calendar-booking-tag"
                        key={booking.id}
                        onClick={() =>
                          setSelectedBooking(booking)
                        }
                        aria-label={`View booking details for ${
                          booking.serviceTitle || "service"
                        }`}>
                        <span>
                          {formatBookingTime(
                            booking.scheduledAt
                          )}
                        </span>
                        <span>
                          {getBookingPersonName(booking)}
                        </span>
                        <span>
                          {booking.serviceTitle || "Service"}
                        </span>
                        <FaCircleInfo aria-hidden="true" />
                      </button>
                    ))}
                  </div>
                </>
              )}
            </div>
          );
        })}
      </div>

      <Modal
        isOpen={selectedBooking !== null}
        onClose={() => setSelectedBooking(null)}>
        {selectedBooking && (
          <div className="calendar-booking-modal">
            <p className="calendar-modal-eyebrow">
              Booking Details
            </p>
            <h2>{selectedBooking.serviceTitle || "Service"}</h2>

            <div className="calendar-modal-details">
              <div>
                <span>{getOppositePersonLabel()}</span>
                <strong>
                  {getBookingPersonName(selectedBooking)}
                </strong>
              </div>
              <div>
                <span>Scheduled</span>
                <strong>
                  {formatBookingDateTime(
                    selectedBooking.scheduledAt
                  )}
                </strong>
              </div>
              <div>
                <span>Price</span>
                <strong>
                  {formatBookingPrice(selectedBooking)}
                </strong>
              </div>
              <div>
                <span>Status</span>
                <strong>
                  {selectedBooking.status || "Scheduled"}
                </strong>
              </div>
            </div>

            <div className="calendar-modal-actions">
              {selectedView === "provider" &&
                selectedBooking.status === "CONFIRMED" && (
                  <button
                    type="button"
                    className="calendar-modal-complete"
                    disabled={isCompleting}
                    onClick={() => void completeBooking()}>
                    {isCompleting
                      ? "Completing..."
                      : "Mark Complete"}
                  </button>
                )}

              <button
                type="button"
                className="calendar-modal-cancel"
                disabled={isCancelling}
                onClick={() => void cancelBooking()}>
                {isCancelling
                  ? "Canceling..."
                  : "Cancel Booking"}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
