import { useMemo } from "react";
import "../Styles/ProfilePage.css";

const LISTINGS_STORAGE_KEY =
  "service-marketplace-profile-listings";

interface ServiceListing {
  id: string;
  title: string;
  description: string;
  price: string;
  isHourly: boolean;
  location: string;
  tags: string[];
}

const defaultProfileServices: ServiceListing[] = [
  {
    id: "calculus-tutoring",
    title: "Calculus Tutoring",
    description:
      "One-on-one support for Calculus I-III, linear algebra, and statistics with flexible evening availability.",
    price: "25",
    isHourly: true,
    location: "Main Library or Online",
    tags: ["Mathematics", "Tutoring", "Online"]
  },
  {
    id: "resume-review",
    title: "Resume Review",
    description:
      "Resume, cover letter, and interview prep for internships, campus roles, and first professional positions.",
    price: "20",
    isHourly: false,
    location: "Student Center or Zoom",
    tags: ["Career", "Resume", "Interview prep"]
  },
  {
    id: "web-portfolio-help",
    title: "Web Portfolio Help",
    description:
      "Portfolio setup and feedback for students who want a cleaner personal site or project showcase.",
    price: "35",
    isHourly: true,
    location: "Remote collaboration",
    tags: ["Web", "Portfolio", "Design"]
  }
];

function formatPrice(price: string, isHourly: boolean) {
  const cleanPrice = price
    .replace(/^\$/, "")
    .replace(/\/hr$/, "")
    .trim();
  const displayPrice = cleanPrice ? `$${cleanPrice}` : "$0";

  return isHourly ? `${displayPrice}/hr` : displayPrice;
}

function normalizeStoredListings(
  storedListings: string
): ServiceListing[] {
  const parsedListings = JSON.parse(storedListings) as Array<
    Partial<ServiceListing> & { status?: string }
  >;

  if (!Array.isArray(parsedListings)) {
    return [];
  }

  return parsedListings
    .filter((listing) => listing.status !== "taken-down")
    .map((listing, index) => {
      const savedPrice = listing.price ?? "";
      const cleanPrice = savedPrice
        .replace(/^\$/, "")
        .replace(/\/hr$/, "")
        .trim();

      return {
        id: listing.id ?? `profile-service-${index}`,
        title: listing.title?.trim() ?? "",
        description: listing.description?.trim() ?? "",
        price: cleanPrice,
        isHourly:
          listing.isHourly ?? savedPrice.endsWith("/hr"),
        location: listing.location?.trim() ?? "",
        tags: Array.isArray(listing.tags) ? listing.tags : []
      };
    })
    .filter(
      (listing) =>
        listing.title &&
        listing.description &&
        listing.price &&
        listing.location
    );
}

function loadProfileServices() {
  const storedListings = window.localStorage.getItem(
    LISTINGS_STORAGE_KEY
  );

  if (!storedListings) {
    return defaultProfileServices;
  }

  try {
    const services = normalizeStoredListings(storedListings);

    return services.length > 0
      ? services
      : defaultProfileServices;
  } catch {
    return defaultProfileServices;
  }
}

function ProfilePage() {
  const services = useMemo(() => loadProfileServices(), []);

  return (
    <main className="profile-screen">
      <header className="profile-header">
        <h1>Profile</h1>
        <p>{services.length} services</p>
      </header>

      <section
        className="profile-section profile-bio"
        aria-label="Bio">
        <h2>Bio</h2>
        <p>
          Your personal information can live here once signup
          and login are ready.
        </p>
      </section>

      <section
        className="profile-section services-section"
        aria-label="Services">
        <div className="section-heading">
          <h2>Services</h2>
          <p>Services shown on this profile.</p>
        </div>

        {services.length === 0 ? (
          <p className="empty-state">
            No services are listed on this profile yet.
          </p>
        ) : (
          <div className="listing-grid">
            {services.map((service) => (
              <article
                className="listing-card"
                key={service.id}>
                <div>
                  <div className="listing-card-heading">
                    <h3>{service.title}</h3>
                  </div>
                  <p className="listing-description">
                    {service.description}
                  </p>
                  <p className="listing-location">
                    {service.location}
                  </p>
                  {service.tags.length > 0 && (
                    <div
                      className="listing-tags"
                      aria-label="Service tags">
                      {service.tags.map((tag) => (
                        <span key={tag}>{tag}</span>
                      ))}
                    </div>
                  )}
                </div>
                <div className="listing-card-footer">
                  <strong>
                    {formatPrice(
                      service.price,
                      service.isHourly
                    )}
                  </strong>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default ProfilePage;
