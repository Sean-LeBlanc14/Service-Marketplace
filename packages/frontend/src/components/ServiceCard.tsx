import { useState } from "react";
import { Badge, Card } from "react-bootstrap";
import ServiceDetailsModal, {
  type ServiceDetails
} from "./ServiceDetailsModal";
import "./styles/ServiceCard.css";

interface Service {
  id: string;
  title: string;
  category: string;
  userId: string;
  priceMin: number;
  priceMax: number;
  priceUnit: string | null;
  description: string;
  location: string;
  tags: string[];
}

interface ServiceCardProps {
  service: Service;
}

function formatPrice(
  priceMin: number,
  priceMax: number,
  priceUnit: string | null
): string {
  const unit = priceUnit ? `/${priceUnit}` : "";

  if (priceMin === priceMax) {
    return `$${priceMin}${unit}`;
  }

  return `$${priceMin}-$${priceMax}${unit}`;
}

function ServiceCard({ service }: ServiceCardProps) {
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);
  const price = formatPrice(
    service.priceMin,
    service.priceMax,
    service.priceUnit
  );
  const modalService: ServiceDetails = {
    id: service.id,
    title: service.title,
    price,
    priceMin: service.priceMin,
    priceMax: service.priceMax,
    description: service.description,
    location: service.location,
    tags: service.tags
  };

  return (
    <>
      <Card className="service-card">
        <Card.Body className="service-card-body">
          <div className="service-card-header">
            <Card.Title className="service-card-title">
              {service.title}
            </Card.Title>
            <Badge bg="none" className="service-tag">
              {service.category}
            </Badge>
          </div>

          <Card.Text className="service-description">
            {service.description}
          </Card.Text>

          <div className="service-location">
            <span
              aria-hidden="true"
              className="service-location-icon">
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
              <Badge
                bg="none"
                key={tag}
                className="service-tag">
                {tag}
              </Badge>
            ))}
          </div>

          <div className="service-card-footer">
            <span className="service-price">{price}</span>
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
          service={modalService}
          onClose={() => setIsDetailsOpen(false)}
        />
      )}
    </>
  );
}

export default ServiceCard;
export type { Service };
