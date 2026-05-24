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
import ServiceCard from "../components/ServiceCard";
import type { Service } from "../components/ServiceCard";
import "../Styles/HomePage.css";
import { API_ENDPOINTS } from "../utils/api";

const CATEGORIES = [
  { value: "All", label: "All Services", icon: LayoutGrid },
  { value: "tutoring", label: "Tutoring", icon: BookOpen },
  { value: "tech", label: "Tech Help", icon: Monitor },
  { value: "housing", label: "Housing", icon: Home },
  { value: "finance", label: "Finance", icon: DollarSign },
  { value: "food and catering", label: "Food & Catering", icon: Utensils },
  { value: "photography", label: "Photography", icon: Camera }
];

const TOKEN_STORAGE_KEY = "jwt_token";

interface ApiService {
  id?: string;
  title?: string;
  category?: string;
  userId?: string;
  providerName?: string;
  priceMin?: number | string | null;
  priceMax?: number | string | null;
  priceUnit?: string | null;
  description?: string;
  location?: string;
  tags?: string[] | null;
}

interface ApiUserProfile {
  email?: string;
  firstName?: string;
  lastName?: string;
  services?: ApiService[];
}

function cleanText(value?: string | null) {
  return value?.trim() ?? "";
}

function cleanPriceValue(value?: number | string | null) {
  return value == null ? "" : String(value).trim();
}

function formatCurrency(value: string) {
  const amount = Number(value);

  if (!Number.isFinite(amount)) {
    return "$0";
  }

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: Number.isInteger(amount) ? 0 : 2
  }).format(amount);
}

function formatPrice(service: ApiService) {
  const minPrice = cleanPriceValue(service.priceMin);
  const maxPrice = cleanPriceValue(service.priceMax);
  const displayPrice =
    minPrice && maxPrice && minPrice !== maxPrice
      ? `${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}`
      : formatCurrency(minPrice || maxPrice);
  const priceUnit = cleanText(service.priceUnit).replace(/^\/+/, "");

  return priceUnit ? `${displayPrice}/${priceUnit}` : displayPrice;
}

function getInitials(name: string) {
  const words = name
    .split(/\s+/)
    .map((word) => word.replace(/[^a-zA-Z0-9]/g, ""))
    .filter(Boolean);

  if (words.length === 0) {
    return "SC";
  }

  return words
    .slice(0, 2)
    .map((word) => word[0])
    .join("")
    .toUpperCase();
}

function getProfileDisplayName(profile: ApiUserProfile) {
  return (
    `${cleanText(profile.firstName)} ${cleanText(profile.lastName)}`.trim() ||
    cleanText(profile.email)
  );
}

function getProfileServiceNames(profile: ApiUserProfile) {
  const displayName = getProfileDisplayName(profile);
  const services = Array.isArray(profile.services) ? profile.services : [];

  if (!displayName) {
    return new Map<string, string>();
  }

  return new Map(
    services
      .map((service) => cleanText(service.id))
      .filter(Boolean)
      .map((serviceId) => [serviceId, displayName])
  );
}

function normalizeService(
  service: ApiService,
  index: number,
  providerNameByServiceId = new Map<string, string>()
) {
  const tags = Array.isArray(service.tags)
    ? service.tags.map(cleanText).filter(Boolean)
    : [];
  const id = cleanText(service.id) || `service-${index}`;
  const providerName =
    cleanText(service.providerName) ||
    providerNameByServiceId.get(id) ||
    "Service creator";
  const priceMin = Number(cleanPriceValue(service.priceMin) || 0);
  const priceMax = Number(cleanPriceValue(service.priceMax) || priceMin);
  const priceUnit = cleanText(service.priceUnit) || null;

  return {
    id,
    title: cleanText(service.title) || "Untitled service",
    category: cleanText(service.category) || "general",
    userId: cleanText(service.userId),
    provider: {
      name: providerName,
      avatar: getInitials(providerName),
      rating: 0,
      reviews: 0
    },
    price: formatPrice(service),
    priceMin,
    priceMax,
    priceUnit,
    description: cleanText(service.description),
    location: cleanText(service.location) || "Campus",
    tags
  };
}

function HomePage() {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [searchQuery, setSearchQuery] = useState("");

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
        const response = await fetch(API_ENDPOINTS.services.services, {
          headers: authHeaders
        });

        if (response.ok) {
          const data = (await response.json()) as ApiService[];
          let providerNameByServiceId = new Map<string, string>();

          try {
            const profileResponse = await fetch(API_ENDPOINTS.user.profile, {
              headers: authHeaders
            });

            if (profileResponse.ok) {
              const profile = (await profileResponse.json()) as ApiUserProfile;
              providerNameByServiceId = getProfileServiceNames(profile);
            }
          } catch {
            providerNameByServiceId = new Map<string, string>();
          }

          const nextServices = Array.isArray(data)
            ? data.map((service, index) =>
                normalizeService(service, index, providerNameByServiceId)
              )
            : [];

          if (isMounted) {
            setServices(nextServices);
            setError("");
          }
        } else if (response.status === 401 || response.status === 403) {
          if (isMounted) {
            setError("Session expired. Please log in again.");
          }
        } else if (isMounted) {
          setError("Failed to load services. Please try again.");
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
  }, []);

  const filteredServices = services.filter((service) => {
    const matchesCategory =
      selectedCategory === "All" || service.category === selectedCategory;

    const query = searchQuery.toLowerCase();
    const matchesSearch =
      query === "" ||
      service.title.toLowerCase().includes(query) ||
      service.description.toLowerCase().includes(query) ||
      service.tags.some((tag) => tag.toLowerCase().includes(query));

    return matchesCategory && matchesSearch;
  });

  return (
    <div className="homepage-wrapper">
      <Container>
        {/* Header */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "24px"
          }}>
          <h1 style={{ fontWeight: "700", fontSize: "1.8rem", margin: 0 }}>
            Campus Services
          </h1>
          <button
            style={{
              backgroundColor: "#003831",
              color: "white",
              border: "none",
              borderRadius: "8px",
              padding: "10px 20px",
              fontWeight: "600",
              cursor: "pointer",
              fontSize: "0.95rem"
            }}>
            List Your Service
          </button>
        </div>

        {/* ST-07: Search bar */}
        <div style={{ position: "relative", marginBottom: "16px" }}>
          <Search
            size={16}
            style={{
              position: "absolute",
              left: "14px",
              top: "50%",
              transform: "translateY(-50%)",
              color: "#888"
            }}
          />
          <input
            type="text"
            placeholder="Search services..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            style={{
              width: "100%",
              padding: "10px 16px 10px 38px",
              borderRadius: "8px",
              border: "1px solid #ccc",
              fontSize: "0.95rem"
            }}
          />
        </div>

        {/* ST-06: Category filter */}
        <div
          style={{
            display: "flex",
            gap: "8px",
            flexWrap: "wrap",
            marginBottom: "16px"
          }}>
          {CATEGORIES.map(({ value, label, icon: Icon }) => {
            const isSelected = selectedCategory === value;
            return (
              <button
                key={value}
                onClick={() => setSelectedCategory(value)}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "6px",
                  padding: "6px 16px",
                  borderRadius: "20px",
                  border: "1px solid #003831",
                  backgroundColor: isSelected ? "#003831" : "white",
                  color: isSelected ? "white" : "#003831",
                  fontWeight: "500",
                  cursor: "pointer",
                  fontSize: "0.85rem"
                }}>
                <Icon size={14} />
                {label}
              </button>
            );
          })}
        </div>

        <p style={{ color: "#666", marginBottom: "24px" }}>
          {filteredServices.length} services found
        </p>

        {loading && <p>Loading services...</p>}
        {error && <p style={{ color: "red" }}>{error}</p>}

        <Row>
          {filteredServices.map((service) => (
            <Col
              key={service.id}
              xs={12}
              md={6}
              lg={4}
              style={{ marginBottom: "24px" }}>
              <ServiceCard service={service} />
            </Col>
          ))}
        </Row>
      </Container>
    </div>
  );
}

export default HomePage;
