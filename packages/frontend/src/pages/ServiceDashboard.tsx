import { API_ENDPOINTS } from "../utils/api";
import type { ApiUserProfile, ApiBooking } from "../utils/types";
import ServiceBooking from "../components/ServiceBooking";
import { toast } from "react-toastify";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/ServiceDashboard.css";

export default function ServiceDashboard() {

  const navigate = useNavigate();

  const [ user, setUser ] = useState<ApiUserProfile>();

  const [ bookingRequests, setBookingRequests ] = useState<ApiBooking[]>() || [];

  const [ serviceHistory, setServiceHistory ] = useState<ApiBooking[]>() || [];

  const [ upcomingBookings, setUpcomingBookings ] = useState<ApiBooking[]>() || [];


  
  useEffect(()=> {
    const authToken = window.localStorage.getItem("jwt_token");

    if (!authToken){
      navigate("/login");
      toast.error("Please login");
    }

    //Gets the user
    const fetchUser = async() => {
      try{
        const response = await fetch(API_ENDPOINTS.user.profile, {
          headers: {
            Authorization: `Bearer ${authToken}`
          }});

          if (!response.ok){
            toast.error("An error occured when fetching your profile");
            return;
          }

          const userData = (await response.json()) as ApiUserProfile;

          setUser(userData);

      }catch(e){
          console.error(e);
          toast.warning("A network error occurred, please try again.");
      }

      
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
          }else if (bookingRequestResponse.status === 401){
            toast.error("Please sign in");
          } else if (bookingRequestResponse.status === 404){
            setBookingRequests([]);
          }else{
            console.log(bookingRequestResponse.status);
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
          }else if (serviceHistoryResponse.status === 404){
            setServiceHistory([]);
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
          }else if (scheduledBookingsResponse.status === 404){
            setUpcomingBookings([]);
          }else{
            toast.warning("A network error occurred");
          }

        }catch(e){
          console.error(e);
          toast.warning("A network error occurred, please reload the page.");
        }
      }
    fetchUser();
    fetchUserServices();
  }, [navigate, setBookingRequests, setServiceHistory, setUpcomingBookings, setUser])


  return (
    <div className="serviceDashboard-wrapper">

      <section>
        <h2>Your Service Requests</h2>

        <div className="booking-container">
          {bookingRequests && bookingRequests.length > 0 ? ( bookingRequests.map((booking: ApiBooking) => (
              <ServiceBooking key={booking.id} booking={booking}/>
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
              <ServiceBooking key={booking.id} booking={booking} user={user} />
            ))
          ) : (
            "No incoming requests"
          )}
        </div>
      </section>

      <section>
        <h2>Your Service History</h2>

        <div className="booking-container">
          {serviceHistory && serviceHistory.length > 0 ? ( serviceHistory.map((booking: ApiBooking) => (
              <ServiceBooking key={booking.id} booking={booking} user={user} />
            ))
          ) : (
            "No incoming requests"
          )}
        </div>
      </section>

    </div>
  );
}
