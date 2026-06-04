import { useEffect, useMemo, useState } from "react";
import { Col, Container, Row } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import ServiceCard from "../components/ServiceCard";
import { ReviewsModal } from "../components/ReviewsModal";
import { API_ENDPOINTS } from "../utils/api";
import {
  cleanText,
  formatProviderRating,
  normalizeRatingValue,
  normalizeReviewCount,
  normalizeService
} from "../utils/serviceFormatting";
import type {
  ApiProviderProfile,
  ApiService
} from "../utils/types";
import "../styles/ProviderProfilePage.css";

const TOKEN_STORAGE_KEY = "jwt_token";

function ProviderProfilePage() {
  const { userId } = useParams();
  const [profile, setProfile] =
    useState<ApiProviderProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showReviews, setShowReviews] = useState(false);

  useEffect(() => {
    let isMounted = true;
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);

    async function fetchProviderProfile() {
      if (!userId) {
        setError("Provider profile not found.");
        setLoading(false);
        return;
      }

      if (!token) {
        setError("Log in to view provider profiles.");
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(
          API_ENDPOINTS.user.providerProfile(userId),
          {
            headers: {
              Authorization: `Bearer ${token}`
            }
          }
        );

        if (!response.ok) {
          throw new Error("Could not load provider profile.");
        }

        const data =
          (await response.json()) as ApiProviderProfile;

        if (isMounted) {
          setProfile(data);
          setError("");
        }
      } catch (profileError) {
        if (isMounted) {
          setError(
            profileError instanceof Error
              ? profileError.message
              : "Could not load provider profile."
          );
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    void fetchProviderProfile();

    return () => {
      isMounted = false;
    };
  }, [userId]);

  const services = useMemo(() => {
    const profileServices = Array.isArray(profile?.services)
      ? profile.services
      : [];

    return profileServices.map((service: ApiService, index) =>
      normalizeService(service, index)
    );
  }, [profile]);

  if (loading) {
    return (
      <main className="provider-profile-screen">
        <p className="provider-profile-empty">
          Loading provider profile...
        </p>
      </main>
    );
  }

  if (error || !profile) {
    return (
      <main className="provider-profile-screen">
        <p className="provider-profile-empty">
          {error || "Provider profile not found."}
        </p>
      </main>
    );
  }

  const displayName =
    `${cleanText(profile.firstName)} ${cleanText(
      profile.lastName
    )}`.trim() || "Service provider";
  const averageRating = normalizeRatingValue(
    profile.averageRating
  );
  const reviewCount = normalizeReviewCount(profile.reviewCount);
  const ratingText = formatProviderRating(
    averageRating,
    reviewCount
  );
  const profileMeta = [
    cleanText(profile.major),
    cleanText(profile.campus)
  ]
    .filter(Boolean)
    .join(" - ");

  return (
    <main className="provider-profile-screen">
      <Container>
        <Link to="/homepage" className="provider-profile-back">
          Back to services
        </Link>

        <header className="provider-profile-header">
          <div>
            <h1>{displayName}</h1>
            {profileMeta && <p>{profileMeta}</p>}
          </div>

          <button
            type="button"
            className="provider-profile-rating-button"
            onClick={() => setShowReviews(true)}
            aria-label={`View ${reviewCount} reviews`}>
            <div className="provider-profile-rating">
              <span aria-hidden="true">{"\u2605"}</span>
              <strong>
                {averageRating?.toFixed(1) ?? "No rating"}
              </strong>
              <p>{ratingText}</p>
            </div>
          </button>
        </header>

        <section className="provider-profile-section">
          <h2>Bio</h2>
          <p>{cleanText(profile.bio) || "No bio yet."}</p>
        </section>

        <section className="provider-profile-section">
          <div className="provider-profile-section-heading">
            <h2>Services</h2>
            <p>{services.length} services</p>
          </div>

          {services.length === 0 ? (
            <p className="provider-profile-empty-inline">
              No services are listed on this profile yet.
            </p>
          ) : (
            <Row>
              {services.map((service) => (
                <Col
                  key={service.id}
                  xs={12}
                  md={6}
                  lg={4}
                  className="provider-profile-service">
                  <ServiceCard service={service} />
                </Col>
              ))}
            </Row>
          )}
        </section>
      </Container>

      {showReviews && userId && (
        <ReviewsModal
          providerId={userId}
          providerName={displayName}
          onClose={() => setShowReviews(false)}
        />
      )}
    </main>
  );
}

export default ProviderProfilePage;
