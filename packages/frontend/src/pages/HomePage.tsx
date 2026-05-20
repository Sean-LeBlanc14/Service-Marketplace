import { useEffect, useState } from "react";
import { Container, Row, Col } from "react-bootstrap";
import { Link } from "react-router-dom";
import ServiceCard from "../components/ServiceCard";
import type { Service } from "../components/ServiceCard";

const SERVICES_API_URL = "http://localhost:8080/api/services";
const USER_PROFILE_API_URL = "http://localhost:8080/api/users/me";
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
  return `${cleanText(profile.firstName)} ${cleanText(profile.lastName)}`.trim()
    || cleanText(profile.email);
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
): Service {
  const tags = Array.isArray(service.tags)
    ? service.tags.map(cleanText).filter(Boolean)
    : [];
  const id = cleanText(service.id) || `service-${index}`;
  const providerName =
    cleanText(service.providerName) ||
    providerNameByServiceId.get(id) ||
    "Service creator";

  return {
    id,
    title: cleanText(service.title) || "Untitled service",
    category: cleanText(service.category) || "general",
    provider: {
      name: providerName,
      avatar: getInitials(providerName),
      rating: 0,
      reviews: 0
    },
    price: formatPrice(service),
    description: cleanText(service.description),
    location: cleanText(service.location) || "Campus",
    tags
  };
}

function HomePage() {
  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function loadServices() {
      try {
        const authToken = localStorage.getItem(TOKEN_STORAGE_KEY);

        if (!authToken) {
          if (isMounted) {
            setServices([]);
            setError("Log in to view campus services.");
          }

          return;
        }

        const authHeaders = {
          Authorization: `Bearer ${authToken}`
        };

        const response = await fetch(SERVICES_API_URL, {
          headers: authHeaders
        });

        if (response.status === 401 || response.status === 403) {
          throw new Error("Log in to view campus services.");
        }

        if (!response.ok) {
          throw new Error("Could not load services.");
        }

        const data = (await response.json()) as ApiService[];
        let providerNameByServiceId = new Map<string, string>();

        try {
          const profileResponse = await fetch(USER_PROFILE_API_URL, {
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
      } catch (loadError) {
        if (isMounted) {
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Could not load services."
          );
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadServices();

    return () => {
      isMounted = false;
    };
  }, []);

  const requiresLogin = error === "Log in to view campus services.";

  return (
    <div style={{ backgroundColor: "#f4f4f4", minHeight: "100vh", padding: "40px 0" }}>
      <Container>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "16px", marginBottom: "24px" }}>
          <h1 style={{ fontWeight: "700", fontSize: "1.8rem", margin: 0 }}>Campus Services</h1>
        </div>

        {isLoading ? (
          <p style={{ color: "#666", marginBottom: "24px" }}>Loading services...</p>
        ) : (
          <p style={{ color: error ? "#9b1c31" : "#666", marginBottom: "24px" }}>
            {error || `${services.length} services found`}
          </p>
        )}

        {!isLoading && requiresLogin && (
          <Link
            to="/login"
            style={{ color: "#003831", fontWeight: 700 }}>
            Log in
          </Link>
        )}

        {!isLoading && !error && services.length === 0 && (
          <p style={{ color: "#666" }}>No services have been listed yet.</p>
        )}

        <Row>
          {services.map((service) => (
            <Col key={service.id} xs={12} md={6} lg={4} style={{ marginBottom: "24px" }}>
              <ServiceCard service={service} />
            </Col>
          ))}
        </Row>
      </Container>
    </div>
  );
}

export default HomePage;
