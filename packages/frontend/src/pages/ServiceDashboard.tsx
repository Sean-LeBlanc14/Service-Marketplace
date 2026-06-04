import { API_ENDPOINTS } from "../utils/api";
import type { ApiBooking } from "../utils/types";
import ServiceBooking from "../components/ServiceBooking";
import DropDown from "../components/DropDown";
import { toast } from "react-toastify";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/ServiceDashboard.css";
import { getToken } from "../utils/helper";

type DashboardView = "requests" | "upcoming" | "completed";

export default function ServiceDashboard() {

  const navigate = useNavigate();

  const [ bookingRequests, setBookingRequests ] = useState<ApiBooking[]>() || [];

  const [ serviceHistory, setServiceHistory ] = useState<ApiBooking[]>() || [];

  const [ upcomingBookings, setUpcomingBookings ] = useState<ApiBooking[]>() || [];

  const [ selectedView, setSelectedView ] = useState<DashboardView>("requests");

  const authToken = getToken();

  useEffect(()=> {

    if (!authToken){
      navigate("/login");
      toast.error("Please login");
    }

  //Will pull up all bookings
  const fetchUserServices = async() => {
        try{

          //Pulls all the requested bookings aka bookings with status AWAITING_PROVIDER_CONFIRMATION
          const bookingRequestResponse = await fetch(API_ENDPOINTS.bookings.getProviderRequests, {
          headers: {
            'Authorization': `Bearer ${authToken}`,
            'Accepted': 'application/json'
          }});

          if (bookingRequestResponse.ok){
            const bookings = (await bookingRequestResponse.json()) as ApiBooking[];

            setBookingRequests(bookings);
          }else{
            toast.warning("Something went wrong");
          }

          //Completed bookings with status completed
          const serviceHistoryResponse = await fetch(API_ENDPOINTS.bookings.getProviderCompleted, {
            headers: {
              'Authorization' : `Bearer ${authToken}`,
              'Accept': 'application/json'
          }});

          if (serviceHistoryResponse.ok){
            const serviceHistory = (await serviceHistoryResponse.json()) as ApiBooking[];
            
            setServiceHistory(serviceHistory);
          }else{
            toast.warning("Something went wrong");
          }

          //Scheduled bookings with status CONFIRMED
          const scheduledBookingsResponse = await fetch(API_ENDPOINTS.bookings.getProviderScheduled, {
             headers: {
              'Authorization' : `Bearer ${authToken}`,
              'Accept': 'application/json'
          }});

          if (scheduledBookingsResponse.ok){
            const scheduledBookings = (await scheduledBookingsResponse.json()) as ApiBooking[];

            setUpcomingBookings(scheduledBookings);
          }else{
            toast.warning("A network error occurred");
          }

        }catch(e){
          console.error(e);
          toast.warning("A network error occurred, please reload the page.");
        }
      }
    fetchUserServices();
  }, [navigate, setBookingRequests, setServiceHistory, setUpcomingBookings, authToken]);

  async function confirmBooking(booking: ApiBooking) {

    try {
      const confirmResponse = await fetch(
        API_ENDPOINTS.bookings.confirm(booking.id as string),
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
        toast.success("Booking accepted, you will recieve payment shortly!");
        setBookingRequests(bookingRequests?.filter((request) => booking.id !== request.id));
        return;
      } else {
        toast.error("Something went wrong");
      }
    } catch {
      //Fail silently
    } 
  }

  async function rejectBooking(booking: ApiBooking) {

    try{
      const rejectionResponse = await fetch(API_ENDPOINTS.bookings.reject(booking.id as string), {
        method: "DELETE",
      headers: {Authorization: `Bearer ${authToken}`,}
    });

    if (rejectionResponse.ok){
      toast.success("Booking successfully rejected.");
      setBookingRequests(bookingRequests?.filter((request) => request.id !== booking.id));
      return;
    }else{
      toast.error("Could not reject this booking.");
    }
    }catch{
      //fail silently
    }

  }

  async function cancelBooking(booking: ApiBooking) {

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
        setUpcomingBookings(upcomingBookings?.filter((appointment) => appointment.id !== booking.id));
        return;
      } else {
        toast.error("Something went wrong");
      }
    } catch (e) {
      toast.warning("A network error occurred, please try again");
      console.error(e);
    } 
  }


  const dashboardViews: Record<DashboardView, {
    heading: string;
    emptyMessage: string;
    bookings: ApiBooking[] | undefined;
  }> = {
    requests: {
      heading: "Your Service Requests",
      emptyMessage: "No incoming requests",
      bookings: bookingRequests
    },
    upcoming: {
      heading: "Your Scheduled Bookings",
      emptyMessage: "No upcoming bookings",
      bookings: upcomingBookings
    },
    completed: {
      heading: "Your Completed Bookings",
      emptyMessage: "No completed bookings",
      bookings: serviceHistory
    }
  };

  const activeView = dashboardViews[selectedView];

  function renderBooking(booking: ApiBooking) {
    if (selectedView === "requests") {
      return (
        <ServiceBooking
          key={booking.id}
          booking={booking}
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
          cancelBooking={cancelBooking}
        />
      );
    }

    return <ServiceBooking key={booking.id} booking={booking}/>;
  }

  return (
    <div className="serviceDashboard-wrapper">
      <h2>Your Service Provider Dashboard</h2>

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
              <option value="requests">Requests</option>
              <option value="upcoming">Upcoming Bookings</option>
              <option value="completed">Completed Bookings</option>
            </>
          }
        />
      </div>

      <section>
        <h2>{activeView.heading}</h2>
        <div className="booking-container">
          {activeView.bookings && activeView.bookings.length > 0 ? (
            activeView.bookings.map((booking: ApiBooking) => renderBooking(booking))
          ) : (
            activeView.emptyMessage
          )}
        </div>
      </section>
    </div>
  );
}
