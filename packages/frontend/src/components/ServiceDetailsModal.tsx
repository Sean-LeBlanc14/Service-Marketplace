import "./Styles/ServiceDetailsModal.css";

interface ServiceDetailsProvider {
  name: string;
  avatar: string;
  rating: number;
  reviews: number;
}

interface ServiceDetails {
  title: string;
  provider: ServiceDetailsProvider;
  price: string;
  description: string;
  location: string;
  tags: string[];
}

interface ServiceDetailsModalProps {
  service: ServiceDetails;
  onClose: () => void;
}

function PinIcon() {
  return (
    <svg viewBox="0 0 24 24" focusable="false" aria-hidden="true">
      <path d="M12 21s7-6.1 7-12A7 7 0 0 0 5 9c0 5.9 7 12 7 12Z" />
      <circle cx="12" cy="9" r="2.4" />
    </svg>
  );
}

function MessageIcon() {
  return (
    <span className="service-details-message-icon" aria-hidden="true">
      <svg viewBox="0 0 24 24" focusable="false">
        <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.8 8.8 0 0 1-3.7-.8L3 21l1.8-5a8.3 8.3 0 0 1-1-4.1 8.4 8.4 0 0 1 8.7-8.1A8.4 8.4 0 0 1 21 11.5Z" />
      </svg>
    </span>
  );
}

function ServiceDetailsModal({
  service,
  onClose
}: ServiceDetailsModalProps) {
  const hasRating = service.provider.rating > 0;

  return (
    <div
      className="service-details-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}>
      <section
        className="service-details-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="service-details-title">
        <div className="service-details-header">
          <h2 id="service-details-title">{service.title}</h2>
          <button
            type="button"
            className="service-details-close"
            aria-label="Close service details"
            onClick={onClose}>
            {"\u00d7"}
          </button>
        </div>

        <div className="service-details-provider">
          <div className="service-details-avatar">
            {service.provider.avatar}
          </div>
          <div>
            <p className="service-details-provider-name">
              {service.provider.name}
            </p>
            {hasRating && (
              <div className="service-details-rating">
                <span className="service-details-star">{"\u2605"}</span>
                <span>{service.provider.rating}</span>
                <span className="service-details-muted">
                  ({service.provider.reviews} reviews)
                </span>
              </div>
            )}
          </div>
        </div>

        <div className="service-details-location">
          <PinIcon />
          <span>{service.location}</span>
        </div>

        <div className="service-details-price">{service.price}</div>

        <section className="service-details-section">
          <h3>Description</h3>
          <p>{service.description}</p>
        </section>

        {service.tags.length > 0 && (
          <section className="service-details-section">
            <h3>Tags</h3>
            <div className="service-details-tags">
              {service.tags.map((tag) => (
                <span key={tag}>{tag}</span>
              ))}
            </div>
          </section>
        )}

        <div className="service-details-actions">
          <button type="button" className="service-details-book">
            Book Now
          </button>
          <button type="button" className="service-details-message">
            <MessageIcon />
            Message
          </button>
        </div>
      </section>
    </div>
  );
}

export default ServiceDetailsModal;
export type { ServiceDetails, ServiceDetailsProvider };
