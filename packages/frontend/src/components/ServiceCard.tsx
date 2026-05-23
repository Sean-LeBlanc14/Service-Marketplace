import { useState } from "react";
import { Badge, Card } from "react-bootstrap";
import ServiceDetailsModal from "./ServiceDetailsModal";
import "./styles/ServiceCard.css";

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
  priceMin: number;
  priceMax: number;
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
      <Card className="service-card">
        <Card.Body className="service-card-body">
          <div className="service-card-header">
            <Card.Title className="service-card-title">
              {service.title}
            </Card.Title>
            <span className="service-provider-avatar">
              {service.provider.avatar}
            </span>
          </div>

          <Card.Text className="service-description">
            {service.description}
          </Card.Text>

          <div className="service-provider-info">
            {service.provider.name}
            {hasRating && (
              <>
                {" "}
                <span className="rating-star">{"\u2605"}</span>{" "}
                <span className="rating-value">
                  {service.provider.rating}
                </span>{" "}
                <span className="review-count">
                  ({service.provider.reviews})
                </span>
              </>
            )}
          </div>

          <div className="service-location">
            <span aria-hidden="true" className="service-location-icon">
              <svg
                className="service-location-svg"
                viewBox="0 0 24 24"
                focusable="false">
                <path d="M12 21s7-6.1 7-12A7 7 0 0 0 5 9c0 5.9 7 12 7 12Z" />
                <circle cx="12" cy="9" r="2.4" />
              </svg>
            </span>
            {service.location}
          </div>

          <div className="service-tags-container">
            {service.tags.map((tag) => (
              <Badge bg="none" key={tag} className="service-tag">
                {tag}
              </Badge>
            ))}
          </div>

          <div className="service-card-footer">
            <span className="service-price">{service.price}</span>
            <button
              type="button"
              onClick={() => setIsDetailsOpen(true)}
              className="btn-view-details">
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
