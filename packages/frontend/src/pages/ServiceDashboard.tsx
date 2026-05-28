import { API_ENDPOINTS } from "../utils/api";
import ServiceCard from "../components/ServiceCard";
import type { ApiService, ApiUserProfile, ApiBooking } from "../utils/types";
import { toast } from "react-toastify";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

export default function ServiceDashboard() {

  const navigate = useNavigate();

  const [ user, setUser ] = useState<ApiUserProfile>();

  const [ bookings, setBookings ] = useState<ApiBooking[]>();

  const [ serviceHistory, setServiceHistory ] = useState<ApiBooking[]>();

  const [ servicePurchaseHistory, setServicePurchaseHistory ] = useState<ApiBooking[]>();



  
  useEffect(()=> {
    const authToken = window.localStorage.getItem("jwt_token");

    if (!authToken){
      navigate("/login");
      toast.error("Please login");
    }

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
  const fetchUserServices = async() => {
        try{
          const bookingResponse = await fetch(API_ENDPOINTS.bookings.getRequests, {
          headers: {
            'Authorization': `Bearer ${authToken}`,
            'Accepted': 'application/json'
          }});

          if (bookingResponse.ok){
            const bookings = (await bookingResponse.json()) as ApiBooking[];

            setBookings(bookings);
          }else if (bookingResponse.status === 401){
            toast.error("Please sign in");
          } else if (bookingResponse.status === 404){
            setBookings([]);
          }else{
            toast.warning("Something went wrong");
          }


          const serviceHistoryResponse = await fetch(API_ENDPOINTS.bookings.getCompleted, {
            headers: {
              'Authorization' : `Bearer ${authToken}`,
              'Accept': 'application/json'
          }});

          if (serviceHistoryResponse.ok){
            const serviceHistory = (await serviceHistoryResponse.json());
            
            setServiceHistory(serviceHistory);
          }else if (serviceHistoryResponse.status === 404){
            toast.warning("No bookings found");
          }

          const scheduledBookingsResponse = await fetch(API_ENDPOINTS.bookings.getScheduled, {
            headers: {"Authorization": `Bearer: ${authToken}`}
          });

          if (scheduledBookingsResponse.ok){
            const scheduledBookings = (await scheduledBookingsResponse.json()) as ApiBooking[];

            setServicePurchaseHistory(scheduledBookings);
          }else if (scheduledBookingsResponse.status === 404){
            toast.warning("No upcoming bookings");
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
  }, [])


  return (
    <div className="serviceDashboard-wrapper">

      <section className="service-request-container">
        <h2>Your Service Requests</h2>
      </section>

      <section className="service-provided-history-container">
        <h2>Your Provided Service History</h2>
      </section>

      <section className="service-history-container">
        <h2>Your Service History</h2>
      </section>

    </div>
  );
}
