import { API_ENDPOINTS } from "../utils/api";
import type { ApiBooking } from "../utils/types";
import ServiceBooking from "../components/ServiceBooking";
import { toast } from "react-toastify";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/ServiceDashboard.css";

export default function ServiceDashboard() {

  const navigate = useNavigate();

  const [ bookingRequests, setBookingRequests ] = useState<ApiBooking[]>() || [];

  const [ serviceHistory, setServiceHistory ] = useState<ApiBooking[]>() || [];

  const [ upcomingBookings, setUpcomingBookings ] = useState<ApiBooking[]>() || [];

  const authToken = window.localStorage.getItem("jwt_token")

  useEffect(()=> {

    if (!authToken){
      navigate("/login");
      toast.error("Please login");
    }

  //Will pull up all bookings
  const fetchUserServices = async() => {
        try{

          //Pulls all the requested bookings aka bookings with status AWAITING_PROVIDER_CONFIRMATION
          const bookingRequestResponse = await fetch(API_ENDPOINTS.bookings.getRequests, {
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
          const serviceHistoryResponse = await fetch(API_ENDPOINTS.bookings.getCompleted, {
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
          const scheduledBookingsResponse = await fetch(API_ENDPOINTS.bookings.getScheduled, {
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
        toast.success("Booking accepted, you will recieve payment shortly!");
        setBookingRequests(bookingRequests?.filter((request) => booking.id !== request.id));
        return;
      } else {
        toast.error("Something went wrong");
      }
    } catch (e) {
      console.error(e);
      toast.warning(
        "A network error occurred when confirming this booking, please try again."
      );
    } 
  }

  async function cancelBooking(booking: ApiBooking) {

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

  return (
    <div className="serviceDashboard-wrapper">

      <section>
        <h2>Your Service Requests</h2>

        <div className="booking-container">
          {bookingRequests && bookingRequests.length > 0 ? ( bookingRequests.map((booking: ApiBooking) => (
              <ServiceBooking key={booking.id} booking={booking} confirmBooking={confirmBooking}/>
            ))
          ) : (
            "No incoming requests"
          )}
        </div>
      </section>

      <section>
        <h2>Your Scheduled Bookings</h2>

        <div className="booking-container">
          {upcomingBookings && upcomingBookings.length > 0 ? ( upcomingBookings.map((booking: ApiBooking) => (
              <ServiceBooking key={booking.id} booking={booking} cancelBooking={cancelBooking}/>
            ))
          ) : (
            "No incoming requests"
          )}
        </div>
      </section>

      <section className="bottom">
        <h2>Your Service History</h2>

        <div className="booking-container">
          {serviceHistory && serviceHistory.length > 0 ? ( serviceHistory.map((booking: ApiBooking) => (
              <ServiceBooking key={booking.id} booking={booking}/>
            ))
          ) : (
            "No incoming requests"
          )}
        </div>
      </section>

    </div>
  );
}
