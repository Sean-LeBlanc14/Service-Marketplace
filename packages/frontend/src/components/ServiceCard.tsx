import { useState } from "react";
import { Badge, Card } from "react-bootstrap";
import ServiceDetailsModal from "./ServiceDetailsModal";
import "./Styles/ServiceCard.css";

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
  const hasRating = service.provider.rating > 0;
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);

  return (
    <>
      <Card
        style={{
          borderRadius: "8px",
          border: "1px solid #dde4ea",
          boxShadow: "0 8px 22px rgba(20, 38, 46, 0.08)",
          height: "100%"
        }}>
        <Card.Body
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "12px",
            padding: "20px"
          }}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "flex-start",
              gap: "12px"
            }}>
            <Card.Title
              style={{
                color: "#071b2d",
                fontSize: "1.1rem",
                fontWeight: "800",
                marginBottom: 0
              }}>
              {service.title}
            </Card.Title>
            <span
              style={{
                fontSize: "0.85rem",
                fontWeight: 700,
                color: "#1f4a3f"
              }}>
              {service.provider.avatar}
            </span>
          </div>

          <Card.Text
            style={{
              color: "#25364a",
              fontSize: "0.95rem",
              marginBottom: 0
            }}>
            {service.description}
          </Card.Text>

          <div
            style={{
              color: "#071b2d",
              fontWeight: "700",
              fontSize: "0.95rem"
            }}>
            {service.provider.name}
            {hasRating && (
              <>
                {" "}
                <span style={{ color: "#d8a20b" }}>{"\u2605"}</span>{" "}
                <span style={{ color: "#071b2d" }}>
                  {service.provider.rating}
                </span>{" "}
                <span style={{ color: "#888", fontWeight: "400" }}>
                  ({service.provider.reviews})
                </span>
              </>
            )}
          </div>

          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              color: "#4c5b6b",
              fontSize: "0.9rem"
            }}>
            <span
              aria-hidden="true"
              style={{
                display: "inline-flex",
                width: "18px",
                height: "18px",
                color: "#667386"
              }}>
              <svg
                viewBox="0 0 24 24"
                focusable="false"
                style={{
                  width: "100%",
                  height: "100%",
                  fill: "none",
                  stroke: "currentColor",
                  strokeLinecap: "round",
                  strokeLinejoin: "round",
                  strokeWidth: 2
                }}>
                <path d="M12 21s7-6.1 7-12A7 7 0 0 0 5 9c0 5.9 7 12 7 12Z" />
                <circle cx="12" cy="9" r="2.4" />
              </svg>
            </span>
            {service.location}
          </div>

          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              gap: "6px"
            }}>
            {service.tags.map((tag) => (
              <Badge
                bg="none"
                key={tag}
                style={{
                  backgroundColor: "#f3f5f7",
                  color: "#334155",
                  fontWeight: "500",
                  fontSize: "0.8rem",
                  borderRadius: "6px",
                  padding: "5px 10px"
                }}>
                {tag}
              </Badge>
            ))}
          </div>

          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginTop: "auto",
              paddingTop: "8px"
            }}>
            <span
              style={{
                fontSize: "1.45rem",
                fontWeight: "800",
                color: "#c89418"
              }}>
              {service.price}
            </span>
            <button
              type="button"
              onClick={() => setIsDetailsOpen(true)}
              style={{
                backgroundColor: "#1f4a3f",
                color: "white",
                border: "none",
                borderRadius: "8px",
                padding: "10px 18px",
                fontWeight: "700",
                cursor: "pointer"
              }}>
              View Details
            </button>
          </div>
        </Card.Body>
      </Card>

      {isDetailsOpen && (
        <ServiceDetailsModal
          service={service}
          onClose={() => setIsDetailsOpen(false)}
        />
      )}
    </>
  );
}

export default ServiceCard;
export type { Service };
