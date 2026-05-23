import { useState, useEffect } from "react";
import { Container, Row, Col } from "react-bootstrap";
import { Search, LayoutGrid, BookOpen, Monitor, Home, DollarSign, Utensils, Camera } from "lucide-react";
import ServiceCard from "../components/ServiceCard";
import type { Service } from "../components/ServiceCard";
import "../Styles/HomePage.css";

const CATEGORIES = [
  { value: "All", label: "All Services", icon: LayoutGrid },
  { value: "tutoring", label: "Tutoring", icon: BookOpen },
  { value: "tech", label: "Tech Help", icon: Monitor },
  { value: "housing", label: "Housing", icon: Home },
  { value: "finance", label: "Finance", icon: DollarSign },
  { value: "food and catering", label: "Food & Catering", icon: Utensils },
  { value: "photography", label: "Photography", icon: Camera },
];

function HomePage() {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    const fetchServices = async () => {
      const token = localStorage.getItem("jwt_token");

      if (!token) {
        setError("You must be logged in to view services.");
        setLoading(false);
        return;
      }

      try {
        const response = await fetch("http://localhost:8080/api/services", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (response.ok) {
          const data = await response.json();
          setServices(data);
        } else if (response.status === 401) {
          setError("Session expired. Please log in again.");
        } else {
          setError("Failed to load services. Please try again.");
        }
      } catch {
        setError("Unable to connect to the server.");
      } finally {
        setLoading(false);
      }
    };

    fetchServices();
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
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "24px" }}>
          <h1 style={{ fontWeight: "700", fontSize: "1.8rem", margin: 0 }}>Campus Services</h1>
          <button
            style={{
              backgroundColor: "#003831",
              color: "white",
              border: "none",
              borderRadius: "8px",
              padding: "10px 20px",
              fontWeight: "600",
              cursor: "pointer",
              fontSize: "0.95rem",
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
              color: "#888",
            }}
          />
          <input
            type="text"
            placeholder="Search services..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: "100%",
              padding: "10px 16px 10px 38px",
              borderRadius: "8px",
              border: "1px solid #ccc",
              fontSize: "0.95rem",
            }}
          />
        </div>

        {/* ST-06: Category filter */}
        <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
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
                  fontSize: "0.85rem",
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
