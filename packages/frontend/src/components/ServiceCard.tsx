import { useState } from "react";
import { Badge, Card } from "react-bootstrap";
import ServiceDetailsModal, {
  type ServiceDetails
} from "./ServiceDetailsModal";
import { formatProviderRating } from "../utils/serviceFormatting";
import "./styles/ServiceCard.css";

interface Service {
  id: string;
  title: string;
  category: string;
  userId: string;
  providerName: string;
  providerAverageRating: number | null;
  providerReviewCount: number;
  price: string;
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

function ServiceCard({ service }: ServiceCardProps) {
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);
  const price = service.price;
  const ratingText = formatProviderRating(
    service.providerAverageRating,
    service.providerReviewCount
  );
  const modalService: ServiceDetails = {
    id: service.id,
    userId: service.userId,
    providerName: service.providerName,
    providerAverageRating: service.providerAverageRating,
    providerReviewCount: service.providerReviewCount,
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

          <div className="service-provider-info">
            <span>{service.providerName}</span>
            <span>
              <span className="rating-star" aria-hidden="true">
                {"\u2605"}
              </span>{" "}
              <span className="rating-value">{ratingText}</span>
            </span>
          </div>

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
