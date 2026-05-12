import { useState } from "react";
import type { FormEvent } from "react";
import "./ProfilePage.css";

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

function formatPrice(price: string, isHourly: boolean) {
  const cleanPrice = price
    .replace(/^\$/, "")
    .replace(/\/hr$/, "")
    .trim();
  const displayPrice = cleanPrice ? `$${cleanPrice}` : "$0";

  return isHourly ? `${displayPrice}/hr` : displayPrice;
}

function cleanPrice(price: string) {
  return price.replace(/^\$/, "").replace(/\/hr$/, "").trim();
}

function parseTags(tags: string) {
  return tags
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);
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

      return {
        id: listing.id ?? `profile-service-${index}`,
        title: listing.title?.trim() ?? "",
        description: listing.description?.trim() ?? "",
        price: cleanPrice(savedPrice),
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
        listing.price
    );
}

function loadProfileServices() {
  const storedListings = window.localStorage.getItem(
    LISTINGS_STORAGE_KEY
  );

  if (!storedListings) {
    return [];
  }

  try {
    return normalizeStoredListings(storedListings);
  } catch {
    return [];
  }
}

function ProfilePage() {
  const [services, setServices] = useState<ServiceListing[]>(
    () => loadProfileServices()
  );
  const [title, setTitle] = useState("");
  const [location, setLocation] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [tags, setTags] = useState("");
  const [isHourly, setIsHourly] = useState(false);
  const [isCreateFormOpen, setIsCreateFormOpen] =
    useState(false);
  const [formMessage, setFormMessage] = useState("");

  function handleCreateListing(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormMessage("");

    const nextTitle = title.trim();
    const nextLocation = location.trim();
    const nextDescription = description.trim();
    const nextPrice = cleanPrice(price);
    const nextTags = parseTags(tags);

    if (!nextTitle || !nextDescription || !nextPrice) {
      setFormMessage("Title, description, and price are required.");
      return;
    }

    const nextListing: ServiceListing = {
      id: `profile-service-${Date.now()}`,
      title: nextTitle,
      description: nextDescription,
      price: nextPrice,
      isHourly,
      location: nextLocation,
      tags: nextTags
    };
    const nextServices = [nextListing, ...services];

    window.localStorage.setItem(
      LISTINGS_STORAGE_KEY,
      JSON.stringify(nextServices)
    );
    setServices(nextServices);
    setTitle("");
    setLocation("");
    setDescription("");
    setPrice("");
    setTags("");
    setIsHourly(false);
    setFormMessage("Listing created.");
  }

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
          <button
            type="button"
            aria-controls="create-listing-form"
            aria-expanded={isCreateFormOpen}
            onClick={() => {
              setIsCreateFormOpen(
                (currentValue) => !currentValue
              );
              setFormMessage("");
            }}
            style={{
              marginTop: "14px",
              border: "none",
              borderRadius: "8px",
              padding: "12px 18px",
              background: "#003831",
              color: "#ffffff",
              font: "inherit",
              fontWeight: 700,
              cursor: "pointer"
            }}>
            {isCreateFormOpen ? "Cancel" : "Create Listing"}
          </button>
        </div>

        {isCreateFormOpen && (
          <form
            id="create-listing-form"
            aria-label="Create service listing"
            onSubmit={handleCreateListing}
            style={{
              display: "grid",
              gap: "12px",
              width: "100%",
              boxSizing: "border-box",
              marginBottom: "24px",
              border: "1px solid #dedede",
              borderRadius: "8px",
              padding: "20px",
              background: "#ffffff"
            }}>
            <div
              style={{
                display: "grid",
                gap: "6px"
              }}>
              <label
                htmlFor="service-title"
                style={{
                  color: "#333333",
                  fontWeight: 700
                }}>
                Title
              </label>
              <input
                id="service-title"
                value={title}
                onChange={(event) =>
                  setTitle(event.target.value)
                }
                placeholder="Service title"
                style={{
                  border: "1px solid #cfcfcf",
                  borderRadius: "8px",
                  padding: "12px",
                  font: "inherit"
                }}
              />
            </div>

            <div
              style={{
                display: "grid",
                gridTemplateColumns:
                  "repeat(auto-fit, minmax(150px, 1fr))",
                gap: "12px",
                alignItems: "end"
              }}>
              <div
                style={{
                  display: "grid",
                  gap: "6px"
                }}>
                <label
                  htmlFor="service-location"
                  style={{
                    color: "#333333",
                    fontWeight: 700
                  }}>
                  Location
                </label>
                <input
                  id="service-location"
                  value={location}
                  onChange={(event) =>
                    setLocation(event.target.value)
                  }
                  placeholder="Main Library or Online"
                  style={{
                    border: "1px solid #cfcfcf",
                    borderRadius: "8px",
                    padding: "12px",
                    font: "inherit"
                  }}
                />
              </div>

              <div
                style={{
                  display: "grid",
                  gap: "6px"
                }}>
                <label
                  htmlFor="service-price"
                  style={{
                    color: "#333333",
                    fontWeight: 700
                  }}>
                  Price
                </label>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    border: "1px solid #cfcfcf",
                    borderRadius: "8px",
                    background: "#ffffff",
                    overflow: "hidden"
                  }}>
                  <span
                    aria-hidden="true"
                    style={{
                      paddingLeft: "12px",
                      color: "#4d4d4d",
                      fontWeight: 700
                    }}>
                    $
                  </span>
                  <input
                    id="service-price"
                    value={price}
                    onChange={(event) =>
                      setPrice(cleanPrice(event.target.value))
                    }
                    placeholder="25"
                    inputMode="decimal"
                    style={{
                      minWidth: 0,
                      flex: 1,
                      border: "none",
                      padding: "12px",
                      font: "inherit",
                      outline: "none"
                    }}
                  />
                </div>
              </div>

              <label
                htmlFor="service-hourly"
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "8px",
                  minHeight: "50px",
                  color: "#333333",
                  fontWeight: 700
                }}>
                <input
                  id="service-hourly"
                  type="checkbox"
                  checked={isHourly}
                  onChange={(event) =>
                    setIsHourly(event.target.checked)
                  }
                  style={{
                    width: "18px",
                    height: "18px",
                    accentColor: "#003831"
                  }}
                />
                Hourly
              </label>
            </div>

            <div
              style={{
                display: "grid",
                gap: "6px"
              }}>
              <label
                htmlFor="service-tags"
                style={{
                  color: "#333333",
                  fontWeight: 700
                }}>
                Tags
              </label>
              <input
                id="service-tags"
                value={tags}
                onChange={(event) =>
                  setTags(event.target.value)
                }
                placeholder="Tutoring, Online, Math"
                style={{
                  border: "1px solid #cfcfcf",
                  borderRadius: "8px",
                  padding: "12px",
                  font: "inherit"
                }}
              />
            </div>

            <div
              style={{
                display: "grid",
                gap: "6px"
              }}>
              <label
                htmlFor="service-description"
                style={{
                  color: "#333333",
                  fontWeight: 700
                }}>
                Description
              </label>
              <textarea
                id="service-description"
                value={description}
                onChange={(event) =>
                  setDescription(event.target.value)
                }
                placeholder="Describe the service you are offering."
                rows={4}
                style={{
                  minHeight: "108px",
                  resize: "vertical",
                  border: "1px solid #cfcfcf",
                  borderRadius: "8px",
                  padding: "12px",
                  font: "inherit"
                }}
              />
            </div>

            {formMessage && (
              <p
                role="status"
                style={{
                  margin: 0,
                  color:
                    formMessage === "Listing created."
                      ? "#0d473f"
                      : "#9f1239",
                  fontWeight: 700
                }}>
                {formMessage}
              </p>
            )}

            <button
              type="submit"
              style={{
                justifySelf: "start",
                border: "none",
                borderRadius: "8px",
                padding: "12px 18px",
                background: "#003831",
                color: "#ffffff",
                font: "inherit",
                fontWeight: 700,
                cursor: "pointer"
              }}>
              Create Listing
            </button>
          </form>
        )}

        {services.length === 0 ? (
          <p className="empty-state">
            No services are listed on this profile yet.
          </p>
        ) : (
          <div className="listing-grid">
            {services.map((service) => (
              <article className="listing-card" key={service.id}>
                <div>
                  <div className="listing-card-heading">
                    <h3>{service.title}</h3>
                  </div>
                  <p className="listing-description">
                    {service.description}
                  </p>
                  {service.location && (
                    <p className="listing-location">
                      {service.location}
                    </p>
                  )}
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
