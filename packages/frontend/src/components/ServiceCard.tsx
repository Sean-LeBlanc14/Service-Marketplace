import { Card, Badge } from "react-bootstrap";
import "../Styles/ServiceCard.css";

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
          {service.provider.name}{" "}
          <span className="rating-star">★ {service.provider.rating}</span>{" "}
          <span className="review-count">({service.provider.reviews})</span>
        </div>

        <div className="service-location">
          📍 {service.location}
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
          <button className="btn-view-details">
            View Details
          </button>
        </div>
      </Card.Body>
    </Card>
  );
}

export default ServiceCard;
export type { Service };
