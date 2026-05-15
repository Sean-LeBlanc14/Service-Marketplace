import { Card, Badge } from "react-bootstrap";

interface Provider {
  name: string;
  avatar: string;
  rating: number;
  reviews: number;
}

interface Service {
  id: string;
  title: string;
  category: string;
  provider: Provider;
  price: string;
  description: string;
  location: string;
  tags: string[];
}

interface ServiceCardProps {
  service: Service;
}

function ServiceCard({ service }: ServiceCardProps) {
  return (
    <Card
      style={{
        borderRadius: "12px",
        border: "1px solid #e0e0e0",
        boxShadow: "0 4px 12px rgba(0,0,0,0.08)",
        height: "100%",
      }}>
      <Card.Body style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
          <Card.Title style={{ fontSize: "1.1rem", fontWeight: "700", marginBottom: 0 }}>
            {service.title}
          </Card.Title>
          <span style={{ fontSize: "1.5rem" }}>{service.provider.avatar}</span>
        </div>

        <Card.Text style={{ color: "#555", fontSize: "0.9rem", marginBottom: 0 }}>
          {service.description}
        </Card.Text>

        <div style={{ fontWeight: "600", fontSize: "0.95rem" }}>
          {service.provider.name}{" "}
          <span style={{ color: "#f5a623" }}>★ {service.provider.rating}</span>{" "}
          <span style={{ color: "#888", fontWeight: "400" }}>({service.provider.reviews})</span>
        </div>

        <div style={{ color: "#666", fontSize: "0.85rem" }}>
          📍 {service.location}
        </div>

        <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
          {service.tags.map((tag, index) => (
            <Badge
              bg="none"
              key={tag}
              style={{
                backgroundColor: "#003831",
                color: "white",
                fontWeight: "400",
                fontSize: "0.8rem",
                borderRadius: "20px",
                padding: "4px 10px",
              }}>
              {tag}
            </Badge>
          ))}
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "auto", paddingTop: "8px" }}>
          <span style={{ fontSize: "1.3rem", fontWeight: "700", color: "#003831" }}>
            {service.price}
          </span>
          <button
            style={{
              backgroundColor: "#003831",
              color: "white",
              border: "none",
              borderRadius: "8px",
              padding: "8px 16px",
              fontWeight: "600",
              cursor: "pointer",
            }}>
            View Details
          </button>
        </div>
      </Card.Body>
    </Card>
  );
}

export default ServiceCard;
export type { Service };