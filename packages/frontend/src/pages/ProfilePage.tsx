import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import "./ProfilePage.css";

const API_URL = "http://localhost:8080/api/users/me";
const SERVICES_API_URL = "http://localhost:8080/api/services";
const TOKEN_STORAGE_KEY = "jwt_token";

const SERVICE_CATEGORY_OPTIONS = [
  { value: "tutoring", label: "Tutoring" },
  { value: "tech help", label: "Tech Help" },
  { value: "housing", label: "Housing" },
  { value: "finance", label: "Finance" },
  { value: "food and catering", label: "Food and Catering" },
  { value: "photography", label: "Photography" }
];

interface ServiceListing {
  id: string;
  title: string;
  category: string;
  description: string;
  priceMin: string;
  priceMax: string;
  priceUnit: string;
  location: string;
  tags: string[];
}

interface UserProfile {
  email: string;
  firstName: string;
  lastName: string;
  major: string;
  campus: string;
  bio: string;
  services: ServiceListing[];
}

interface ApiServiceListing {
  id?: string;
  title?: string;
  category?: string;
  description?: string;
  priceMin?: number | string | null;
  priceMax?: number | string | null;
  priceUnit?: string | null;
  location?: string;
  tags?: string[];
}

interface ApiUserProfile {
  email?: string;
  firstName?: string;
  lastName?: string;
  major?: string;
  campus?: string;
  bio?: string;
  services?: ApiServiceListing[];
}

const emptyProfile: UserProfile = {
  email: "",
  firstName: "",
  lastName: "",
  major: "",
  campus: "",
  bio: "",
  services: []
};

function cleanText(value?: string) {
  return value?.trim() ?? "";
}

function cleanPriceValue(value?: number | string | null) {
  return value == null ? "" : String(value).trim();
}

function formatCurrency(value: string) {
  const amount = Number(value);

  if (!Number.isFinite(amount)) {
    return "$0";
  }

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: Number.isInteger(amount) ? 0 : 2
  }).format(amount);
}

function formatPrice(service: ServiceListing) {
  const minPrice = service.priceMin || service.priceMax;
  const maxPrice = service.priceMax || service.priceMin;
  const displayPrice =
    minPrice && maxPrice && minPrice !== maxPrice
      ? `${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}`
      : formatCurrency(minPrice || maxPrice);
  const cleanUnit = service.priceUnit.replace(/^\/+/, "");

  return cleanUnit
    ? `${displayPrice}/${cleanUnit}`
    : displayPrice;
}

function formatCategory(category: string) {
  return (
    SERVICE_CATEGORY_OPTIONS.find((option) => option.value === category)?.label ||
    category
  );
}

function normalizeServices(
  services: ApiServiceListing[] | undefined
): ServiceListing[] {
  if (!Array.isArray(services)) {
    return [];
  }

  return services.map((service, index) => ({
    id: cleanText(service.id) || `profile-service-${index}`,
    title: cleanText(service.title),
    category: cleanText(service.category),
    description: cleanText(service.description),
    priceMin: cleanPriceValue(service.priceMin),
    priceMax: cleanPriceValue(service.priceMax),
    priceUnit: cleanText(service.priceUnit ?? undefined),
    location: cleanText(service.location),
    tags: Array.isArray(service.tags)
      ? service.tags.map(cleanText).filter(Boolean)
      : []
  }));
}

function normalizeProfile(
  profile: ApiUserProfile
): UserProfile {
  return {
    email: cleanText(profile.email),
    firstName: cleanText(profile.firstName),
    lastName: cleanText(profile.lastName),
    major: cleanText(profile.major),
    campus: cleanText(profile.campus),
    bio: cleanText(profile.bio),
    services: normalizeServices(profile.services)
  };
}

function ProfilePage() {
  const [profile, setProfile] =
    useState<UserProfile>(emptyProfile);
  const [authToken] = useState(() =>
    window.localStorage.getItem(TOKEN_STORAGE_KEY)
  );
  const [bioDraft, setBioDraft] = useState("");
  const [bioMessage, setBioMessage] = useState("");
  const [error, setError] = useState(() =>
    authToken ? "" : "Log in to view your profile."
  );
  const [isEditingBio, setIsEditingBio] = useState(false);
  const [isLoading, setIsLoading] = useState(
    Boolean(authToken)
  );
  const [isSaving, setIsSaving] = useState(false);
  const [serviceDescription, setServiceDescription] =
    useState("");
  const [serviceError, setServiceError] = useState("");
  const [serviceMessage, setServiceMessage] = useState("");
  const [serviceTitle, setServiceTitle] = useState("");
  const [serviceCategory, setServiceCategory] = useState("");
  const [servicePricingType, setServicePricingType] =
    useState("flat");
  const [isCreatingService, setIsCreatingService] =
    useState(false);
  const [isServiceFormOpen, setIsServiceFormOpen] =
    useState(false);
  const [servicePrice, setServicePrice] = useState("");
  const [serviceMinPrice, setServiceMinPrice] = useState("");
  const [serviceMaxPrice, setServiceMaxPrice] = useState("");
  const [servicePriceUnit, setServicePriceUnit] = useState("");
  const [serviceLocation, setServiceLocation] = useState("");
  const [serviceTags, setServiceTags] = useState("");

  useEffect(() => {
    let isMounted = true;

    if (!authToken) {
      return;
    }

    async function loadProfile() {
      try {
        const response = await fetch(API_URL, {
          headers: {
            Authorization: `Bearer ${authToken}`
          }
        });

        if (!response.ok) {
          throw new Error("Could not load your profile.");
        }

        const data = (await response.json()) as ApiUserProfile;
        const nextProfile = normalizeProfile(data);

        if (isMounted) {
          setProfile(nextProfile);
          setBioDraft(nextProfile.bio);
          setIsEditingBio(false);
        }
      } catch {
        if (isMounted) {
          setError("Could not load your profile.");
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadProfile();

    return () => {
      isMounted = false;
    };
  }, [authToken]);

  async function handleBioSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    const nextBio = bioDraft.trim();

    if (!authToken) {
      setError("Log in to save your profile.");
      return;
    }

    setIsSaving(true);
    setError("");
    setBioMessage("");

    try {
      const response = await fetch(API_URL, {
        method: "PATCH",
        headers: {
          Authorization: `Bearer ${authToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ bio: nextBio })
      });

      if (!response.ok) {
        throw new Error("Could not save your bio.");
      }

      const data = (await response.json()) as ApiUserProfile;
      const nextProfile = normalizeProfile(data);

      setProfile(nextProfile);
      setBioDraft(nextProfile.bio);
      setIsEditingBio(false);
      setBioMessage("Bio saved.");
    } catch {
      setError("Could not save your bio.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleServiceSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (!authToken) {
      setServiceError("Log in to create a listing.");
      return;
    }

    setServiceError("");
    setServiceMessage("");

    try {
      const tags = serviceTags
        .split(",")
        .map((tag) => tag.trim())
        .filter((tag) => tag.length > 0);
      const priceText = servicePrice.trim();
      const minPriceText = serviceMinPrice.trim();
      const maxPriceText = serviceMaxPrice.trim();
      const locationText = serviceLocation.trim();
      let priceMin = 0;
      let priceMax = 0;

      if (!serviceCategory) {
        setServiceError("Choose a category.");
        return;
      }

      if (servicePricingType === "flat") {
        priceMin = Number(priceText);
        priceMax = priceMin;

        if (!priceText) {
          setServiceError("Enter a price.");
          return;
        }

        if (!Number.isFinite(priceMin)) {
          setServiceError("Enter a valid price.");
          return;
        }
      } else {
        priceMin = Number(minPriceText);
        priceMax = Number(maxPriceText);

        if (!minPriceText || !maxPriceText) {
          setServiceError("Enter both a minimum and maximum price.");
          return;
        }

        if (!Number.isFinite(priceMin) || !Number.isFinite(priceMax)) {
          setServiceError("Enter valid prices.");
          return;
        }

        if (priceMax < priceMin) {
          setServiceError("Price max must be greater than or equal to price min.");
          return;
        }
      }

      if (!locationText) {
        setServiceError("Enter a location.");
        return;
      }

      const requestBody: Record<string, unknown> = {
        title: serviceTitle.trim(),
        category: serviceCategory,
        priceMin: priceMin,
        priceMax: priceMax,
        priceUnit: servicePriceUnit.trim(),
        description: serviceDescription.trim(),
        location: locationText,
        tags: tags
      };

      setIsCreatingService(true);

      const response = await fetch(SERVICES_API_URL, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${authToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) {
        throw new Error("Could not create listing.");
      }

      const data = (await response.json()) as ApiServiceListing;
      const [createdService] = normalizeServices([data]);

      if (createdService) {
        setProfile((currentProfile) => ({
          ...currentProfile,
          services: [...currentProfile.services, createdService]
        }));
      }

      setServiceTitle("");
      setServiceCategory("");
      setServiceDescription("");
      setServicePricingType("flat");
      setServicePrice("");
      setServiceMinPrice("");
      setServiceMaxPrice("");
      setServicePriceUnit("");
      setServiceLocation("");
      setServiceTags("");
      setServiceMessage("Listing created.");
      setIsServiceFormOpen(false);
    } catch {
      setServiceError("Could not create listing.");
    } finally {
      setIsCreatingService(false);
    }
  }

  function handleCancelServiceForm() {
    setServiceTitle("");
    setServiceCategory("");
    setServiceDescription("");
    setServicePricingType("flat");
    setServicePrice("");
    setServiceMinPrice("");
    setServiceMaxPrice("");
    setServicePriceUnit("");
    setServiceLocation("");
    setServiceTags("");
    setServiceError("");
    setIsServiceFormOpen(false);
  }

  const displayName =
    `${profile.firstName} ${profile.lastName}`.trim() ||
    "Profile";

  if (isLoading) {
    return (
      <main className="profile-screen">
        <p className="empty-state">Loading profile...</p>
      </main>
    );
  }

  if (error && !profile.email) {
    return (
      <main className="profile-screen">
        <p className="empty-state">{error}</p>
      </main>
    );
  }

  return (
    <main className="profile-screen">
      <header className="profile-header">
        <div>
          <h1>{displayName}</h1>
          <p>{profile.email}</p>
          {(profile.major || profile.campus) && (
            <p>
              {[profile.major, profile.campus]
                .filter(Boolean)
                .join(" - ")}
            </p>
          )}
        </div>
        <p>{profile.services.length} services</p>
      </header>

      <section
        className="profile-section profile-bio"
        aria-label="Bio">
        <div className="section-heading">
          <div>
            <h2>Bio</h2>
            <p>Profile bio shown with your services.</p>
          </div>
          {!isEditingBio && ( //testing
            <button
              type="button"
              className="section-action-button"
              onClick={() => {
                setBioDraft(profile.bio);
                setBioMessage("");
                setError("");
                setIsEditingBio(true);
              }}>
              Edit Bio
            </button>
          )}
        </div>
        {profile.bio ? (
          <p>{profile.bio}</p>
        ) : (
          <p>Write a short bio for your profile.</p>
        )}
        {(bioMessage || error) && (
          <p
            role="status"
            style={{
              marginTop: "10px",
              color: error ? "#9b1c31" : "#0d473f",
              fontWeight: 700
            }}>
            {error || bioMessage}
          </p>
        )}
        {isEditingBio ? (
          <form
            aria-label="Edit profile bio"
            onSubmit={handleBioSubmit}
            style={{
              display: "grid",
              gap: "12px",
              marginTop: "16px"
            }}>
            <textarea
              aria-label="Profile bio"
              value={bioDraft}
              onChange={(event) => {
                setBioDraft(event.target.value);
                setBioMessage("");
                setError("");
              }}
              placeholder="Bio"
              rows={4}
              style={{
                boxSizing: "border-box",
                width: "100%",
                minHeight: "110px",
                resize: "vertical",
                border: "1px solid #cfcfcf",
                borderRadius: "8px",
                padding: "12px",
                font: "inherit"
              }}
            />
            <div
              style={{
                display: "flex",
                flexWrap: "wrap",
                gap: "10px"
              }}>
              <button
                type="submit"
                disabled={isSaving}
                style={{
                  border: "none",
                  borderRadius: "8px",
                  padding: "12px 18px",
                  background: "#003831",
                  color: "#ffffff",
                  font: "inherit",
                  fontWeight: 700,
                  cursor: "pointer"
                }}>
                {isSaving ? "Saving..." : "Save Bio"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setBioDraft(profile.bio);
                  setBioMessage("");
                  setError("");
                  setIsEditingBio(false);
                }}
                style={{
                  border: "1px solid #b8b8b8",
                  borderRadius: "8px",
                  padding: "12px 18px",
                  background: "#ffffff",
                  color: "#161616",
                  font: "inherit",
                  fontWeight: 700,
                  cursor: "pointer"
                }}>
                Cancel
              </button>
            </div>
          </form>
        ) : null}
      </section>

      <section
        className="profile-section services-section"
        aria-label="Services">
        <div className="section-heading">
          <div>
            <h2>Services</h2>
            <p>Services shown on this profile.</p>
          </div>
          {!isServiceFormOpen && (
            <button
              type="button"
              className="section-action-button"
              onClick={() => {
                setServiceError("");
                setServiceMessage("");
                setServiceLocation("");
                setIsServiceFormOpen(true);
              }}>
              Create Service
            </button>
          )}
        </div>

        {!isServiceFormOpen && serviceMessage && (
          <p role="status" className="form-success">
            {serviceMessage}
          </p>
        )}

        {isServiceFormOpen && (
          <form
            className="service-form"
            aria-label="Create service listing"
            onSubmit={handleServiceSubmit}>
            <div className="service-form-grid">
              <label>
                <span>Title</span>
                <input
                  value={serviceTitle}
                  onChange={(event) => {
                    setServiceTitle(event.target.value);
                    setServiceError("");
                    setServiceMessage("");
                  }}
                  placeholder="Calculus tutoring"
                  required
                />
              </label>

              <label>
                <span>Category</span>
                <select
                  value={serviceCategory}
                  onChange={(event) => {
                    setServiceCategory(event.target.value);
                    setServiceError("");
                    setServiceMessage("");
                  }}
                  required>
                  <option value="" disabled>
                    Select a category
                  </option>
                  {SERVICE_CATEGORY_OPTIONS.map((category) => (
                    <option
                      key={category.value}
                      value={category.value}>
                      {category.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="service-pricing">
              <div className="service-pricing-heading">
                <span>Pricing</span>
                <div
                  className="pricing-toggle"
                  aria-label="Choose pricing type">
                  <button
                    type="button"
                    className={
                      servicePricingType === "flat" ? "active" : ""
                    }
                    aria-pressed={servicePricingType === "flat"}
                    onClick={() => {
                      setServicePricingType("flat");
                      setServiceError("");
                      setServiceMessage("");
                    }}>
                    Flat Price
                  </button>
                  <button
                    type="button"
                    className={
                      servicePricingType === "range" ? "active" : ""
                    }
                    aria-pressed={servicePricingType === "range"}
                    onClick={() => {
                      setServicePricingType("range");
                      setServiceError("");
                      setServiceMessage("");
                    }}>
                    Range
                  </button>
                </div>
              </div>

              <div
                className={`service-form-grid service-price-grid ${
                  servicePricingType === "flat"
                    ? "service-price-grid-flat"
                    : ""
                }`}>
                {servicePricingType === "flat" ? (
                  <label>
                    <span>Price</span>
                    <input
                      value={servicePrice}
                      onChange={(event) => {
                        setServicePrice(event.target.value);
                        setServiceError("");
                        setServiceMessage("");
                      }}
                      min="0"
                      step="0.01"
                      type="number"
                      placeholder="$"
                      required
                    />
                  </label>
                ) : (
                  <>
                    <label>
                      <span>Price Min</span>
                      <input
                        value={serviceMinPrice}
                        onChange={(event) => {
                          setServiceMinPrice(event.target.value);
                          setServiceError("");
                          setServiceMessage("");
                        }}
                        min="0"
                        step="0.01"
                        type="number"
                        placeholder="$"
                        required
                      />
                    </label>

                    <label>
                      <span>Price Max</span>
                      <input
                        value={serviceMaxPrice}
                        onChange={(event) => {
                          setServiceMaxPrice(event.target.value);
                          setServiceError("");
                          setServiceMessage("");
                        }}
                        min="0"
                        step="0.01"
                        type="number"
                        placeholder="$$$"
                        required
                      />
                    </label>
                  </>
                )}

                <label>
                  <span>Price Unit (optional)</span>
                  <input
                    value={servicePriceUnit}
                    onChange={(event) => {
                      setServicePriceUnit(event.target.value);
                      setServiceError("");
                      setServiceMessage("");
                    }}
                    placeholder="e.g. hour, session, meal"
                  />
                </label>
              </div>
            </div>

            <label>
              <span>Description</span>
              <textarea
                value={serviceDescription}
                onChange={(event) => {
                  setServiceDescription(event.target.value);
                  setServiceError("");
                  setServiceMessage("");
                }}
                placeholder="Describe what you are offering"
                rows={4}
                required
              />
            </label>

            <label>
              <span>Location</span>
              <input
                value={serviceLocation}
                onChange={(event) => {
                  setServiceLocation(event.target.value);
                  setServiceError("");
                  setServiceMessage("");
                }}
                placeholder="Campus or address"
                required
              />
            </label>

            <label>
              <span>Tags (optional)</span>
              <textarea
                value={serviceTags}
                onChange={(event) => {
                  setServiceTags(event.target.value);
                  setServiceError("");
                  setServiceMessage("");
                }}
                placeholder="e.g. tutoring, math, calculus"
                rows={2}
              />
            </label>

            {serviceError && (
              <p role="status" className="form-error">
                {serviceError}
              </p>
            )}

            <div className="service-form-actions">
              <button
                type="submit"
                disabled={isCreatingService}>
                {isCreatingService
                  ? "Creating..."
                  : "Create Service"}
              </button>
              <button
                type="button"
                className="secondary-button"
                onClick={handleCancelServiceForm}>
                Cancel
              </button>
            </div>
          </form>
        )}

        {profile.services.length === 0 ? (
          <p className="empty-state">
            No services are listed on this profile yet.
          </p>
        ) : (
          <div className="listing-grid">
            {profile.services.map((service) => (
              <article
                className="listing-card"
                key={service.id}>
                <div>
                  <div className="listing-card-heading">
                    <h3>{service.title}</h3>
                  </div>
                  {service.category && (
                    <p className="listing-category">
                      {formatCategory(service.category)}
                    </p>
                  )}
                  <p className="listing-description">
                    {service.description}
                  </p>
                  <p className="listing-location">
                    <span aria-hidden="true" className="listing-location-pin">
                      <svg viewBox="0 0 24 24" focusable="false">
                        <path d="M12 21s7-6.1 7-12A7 7 0 0 0 5 9c0 5.9 7 12 7 12Z" />
                        <circle cx="12" cy="9" r="2.4" />
                      </svg>
                    </span>
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
                  <strong>{formatPrice(service)}</strong>
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
