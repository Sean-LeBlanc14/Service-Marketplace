import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import "../Styles/ProfilePage.css";

const API_URL = "http://localhost:8080/api/users/me";
const TOKEN_STORAGE_KEY = "jwt_token";

interface ServiceListing {
  id: string;
  title: string;
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

function normalizeServices(
  services: ApiServiceListing[] | undefined
): ServiceListing[] {
  if (!Array.isArray(services)) {
    return [];
  }

  return services.map((service, index) => ({
    id: cleanText(service.id) || `profile-service-${index}`,
    title: cleanText(service.title),
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
        <h2>Bio</h2>
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
        ) : (
          <button
            type="button"
            onClick={() => {
              setBioDraft(profile.bio);
              setBioMessage("");
              setError("");
              setIsEditingBio(true);
            }}
            style={{
              border: "none",
              borderRadius: "8px",
              marginTop: "16px",
              padding: "12px 18px",
              background: "#003831",
              color: "#ffffff",
              font: "inherit",
              fontWeight: 700,
              cursor: "pointer"
            }}>
            Edit Bio
          </button>
        )}
      </section>

      <section
        className="profile-section services-section"
        aria-label="Services">
        <div className="section-heading">
          <h2>Services</h2>
          <p>Services shown on this profile.</p>
        </div>

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
