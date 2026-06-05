import { useEffect, useState } from "react";
import { Container, Row, Col } from "react-bootstrap";
import {
  Search,
  LayoutGrid,
  BookOpen,
  Monitor,
  Home,
  DollarSign,
  Utensils,
  Camera
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import ServiceCard from "../components/ServiceCard";
import type { Service } from "../components/ServiceCard";
import "../styles/HomePage.css";
import { API_ENDPOINTS } from "../utils/api";
import { toast } from "react-toastify";
import type {
  ApiUserProfile,
  ApiService
} from "../utils/types";
import { normalizeService } from "../utils/serviceFormatting";

const CATEGORIES = [
  { value: "All", label: "All Services", icon: LayoutGrid },
  { value: "tutoring", label: "Tutoring", icon: BookOpen },
  { value: "tech", label: "Tech Help", icon: Monitor },
  { value: "housing", label: "Housing", icon: Home },
  { value: "finance", label: "Finance", icon: DollarSign },
  {
    value: "food and catering",
    label: "Food & Catering",
    icon: Utensils
  },
  { value: "photography", label: "Photography", icon: Camera }
];

const TOKEN_STORAGE_KEY = "jwt_token";

function HomePage() {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedCategory, setSelectedCategory] =
    useState("All");
  const [searchQuery, setSearchQuery] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    let isMounted = true;

    const fetchServices = async () => {
      const token = localStorage.getItem(TOKEN_STORAGE_KEY);

      if (!token) {
        if (isMounted) {
          setServices([]);
          setError("You must be logged in to view services.");
          setLoading(false);
        }
        return;
      }

      const authHeaders = {
        Authorization: `Bearer ${token}`
      };

      try {
        const response = await fetch(
          API_ENDPOINTS.services.services,
          {
            headers: authHeaders
          }
        );

        if (response.ok) {
          const data = (await response.json()) as ApiService[];

          try {
            const profileResponse = await fetch(
              API_ENDPOINTS.user.profile,
              {
                headers: authHeaders
              }
            );

            if (profileResponse.ok) {
              const profile =
                (await profileResponse.json()) as ApiUserProfile;

              if (!profile.verified) {
                toast.warning(
                  "Please verify your account before proceeding."
                );
                navigate("/verify");
              }
            }
          } catch (e) {
            console.error(e);
            toast.warning(
              "A network error occured, please try again"
            );
          }

          const nextServices = Array.isArray(data)
            ? data.map((service, index) =>
                normalizeService(service, index)
              )
            : [];

          if (isMounted) {
            setServices(nextServices);
            setError("");
          }
        } else if (
          response.status === 401 ||
          response.status === 403
        ) {
          if (isMounted) {
            setError("Session expired. Please log in again.");
          }
        } else if (isMounted) {
          setError(
            "Failed to load services. Please try again."
          );
        }
      } catch {
        if (isMounted) {
          setError("Unable to connect to the server.");
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    void fetchServices();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const filteredServices = services.filter((service) => {
    const matchesCategory =
      selectedCategory === "All" ||
      service.category === selectedCategory;

    const query = searchQuery.toLowerCase();
    const matchesSearch =
      query === "" ||
      service.title.toLowerCase().includes(query) ||
      service.description.toLowerCase().includes(query) ||
      service.tags.some((tag) =>
        tag.toLowerCase().includes(query)
      );

    return matchesCategory && matchesSearch;
  });

  return (
    <div className="homepage-wrapper">
      <Container>
        {/* Header */}
        <div className="heading-container">
          <h1 className="heading">Campus Services</h1>
          <button className="homepage-list-service-button">
            List Your Service
          </button>
        </div>

        {/* ST-07: Search bar */}
        <div className="homepage-search">
          <Search size={16} className="homepage-search-icon" />
          <input
            type="text"
            placeholder="Search services..."
            value={searchQuery}
            onChange={(event) =>
              setSearchQuery(event.target.value)
            }
            className="homepage-search-input"
          />
        </div>

        {/* ST-06: Category filter */}
        <div className="homepage-categories">
          {CATEGORIES.map(({ value, label, icon: Icon }) => {
            const isSelected = selectedCategory === value;
            return (
              <button
                key={value}
                onClick={() => setSelectedCategory(value)}
                className={`homepage-category-button${
                  isSelected
                    ? " homepage-category-button-selected"
                    : ""
                }`}>
                <Icon size={14} />
                {label}
              </button>
            );
          })}
        </div>

        {!loading && (
          <p className="status-message">
            {filteredServices.length} services found
          </p>
        )}

        {loading && (
          <p className="status-message">Loading services...</p>
        )}
        {error && (
          <p className="status-message status-message-error">
            {error}
          </p>
        )}

        <Row>
          {filteredServices.map((service) => (
            <Col
              key={service.id}
              xs={12}
              md={6}
              lg={4}
              className="service-padding">
              <ServiceCard service={service} />
            </Col>
          ))}
        </Row>
      </Container>
    </div>
  );
}

export default HomePage;
