import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import "../styles/ProfilePage.css";
import { API_ENDPOINTS } from "../utils/api";
import {
  formatPriceUnit,
  normalizePriceUnit,
  PRICE_UNIT_OPTIONS
} from "../utils/pricing";
import { toast } from "react-toastify";
import type { ApiUserProfile, ApiService } from "../utils/types";
import { useNavigate } from "react-router-dom";

const TOKEN_STORAGE_KEY = "jwt_token";

const SERVICE_CATEGORY_OPTIONS = [
  { value: "tutoring", label: "Tutoring" },
  { value: "tech help", label: "Tech Help" },
  { value: "housing", label: "Housing" },
  { value: "finance", label: "Finance" },
  { value: "food and catering", label: "Food and Catering" },
  { value: "photography", label: "Photography" }
];

const NO_PRICE_UNIT_VALUE = "__none__";

const SERVICE_TITLE_MAX_LENGTH = 80;
const SERVICE_DESCRIPTION_MAX_LENGTH = 1000;
const SERVICE_TAG_MAX_COUNT = 5;
const SERVICE_TAG_MAX_LENGTH = 50;

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


interface ConnectStatus {
  accountId: string | null;
  chargesEnabled: boolean;
  detailsSubmitted: boolean;
  payoutsEnabled: boolean;
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

const PRICE_INPUT_PATTERN = /^\d*(?:\.\d{0,2})?$/;

function cleanText(value?: string) {
  return value?.trim() ?? "";
}

function cleanPriceValue(value?: number | string | null) {
  return value == null ? "" : String(value).trim();
}

function isPriceInputValue(value: string) {
  return PRICE_INPUT_PATTERN.test(value);
}

function normalizeTag(value: string) {
  return value
    .trim()
    .replace(/\s+/g, " ")
    .toLocaleLowerCase()
    .split(" ")
    .map((word) =>
      word
        ? `${word.charAt(0).toLocaleUpperCase()}${word.slice(1)}`
        : word
    )
    .join(" ");
}

function parseServiceTags(value: string) {
  return value
    .split(",")
    .map(normalizeTag)
    .filter((tag) => tag.length > 0);
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
  const priceUnit = normalizePriceUnit(service.priceUnit);

  return priceUnit ? `${displayPrice}/${priceUnit}` : displayPrice;
}

function formatCategory(category: string) {
  return (
    SERVICE_CATEGORY_OPTIONS.find(
      (option) => option.value === category
    )?.label || category
  );
}

function normalizeServices(
  services: ApiService[] | undefined
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
  const navigate = useNavigate();
  const [connectStatus, setConnectStatus] = useState<ConnectStatus | null>(null);
  const [isConnecting, setIsConnecting] = useState(false);
  const [editingServiceId, setEditingServiceId] = useState<
    string | null
  >(null);
  const [deletingServiceId, setDeletingServiceId] = useState<
    string | null
  >(null);
  const [servicePendingDeletion, setServicePendingDeletion] =
    useState<ServiceListing | null>(null);

  const isEditingService = editingServiceId !== null;
  const hasCustomPriceUnit =
    servicePriceUnit !== "" &&
    servicePriceUnit !== NO_PRICE_UNIT_VALUE &&
    !PRICE_UNIT_OPTIONS.some(
      (unit) => unit.value === servicePriceUnit
    );

  useEffect(() => {
    let isMounted = true;

    if (!authToken) {
      return;
    }

    async function loadProfile() {
      try {
        const response = await fetch(
          API_ENDPOINTS.user.profile,
          {
            headers: {
              Authorization: `Bearer ${authToken}`
            }
          }
        );

        if (!response.ok) {
          toast.error("Could not load your profile.");
        }

        const data = (await response.json()) as ApiUserProfile;

        if (!data.verified){
          toast.warning("Please verify your account before proceeding.");
          navigate("/verify");
        }
        const nextProfile = normalizeProfile(data);

        const connectResponse = await fetch(API_ENDPOINTS.payments.connectStatus, {
          headers: { Authorization: `Bearer ${authToken}` }
        });

        if (isMounted) {
          setProfile(nextProfile);
          setBioDraft(nextProfile.bio);
          setIsEditingBio(false);
          if (connectResponse.ok) {
            setConnectStatus((await connectResponse.json()) as ConnectStatus);
          }
        }
      } catch {
        if (isMounted) {
          toast.error("Could not load your profile.");
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
  }, [authToken, navigate]);

  useEffect(() => {
    if (!servicePendingDeletion) {
      return;
    }

    const pendingServiceId = servicePendingDeletion.id;

    function handleKeyDown(event: KeyboardEvent) {
      if (
        event.key === "Escape" &&
        deletingServiceId !== pendingServiceId
      ) {
        setServicePendingDeletion(null);
      }
    }

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [deletingServiceId, servicePendingDeletion]);

  async function handleBioSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    const nextBio = bioDraft.trim();

    if (!authToken) {
      toast.error("Log in to save your profile.");
      return;
    }

    setIsSaving(true);
    setError("");
    setBioMessage("");

    try {
      const response = await fetch(API_ENDPOINTS.user.profile, {
        method: "PATCH",
        headers: {
          Authorization: `Bearer ${authToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ bio: nextBio })
      });

      if (!response.ok) {
        toast.error("Could not save your bio.");
      }

      const data = (await response.json()) as ApiUserProfile;
      const nextProfile = normalizeProfile(data);

      setProfile(nextProfile);
      setBioDraft(nextProfile.bio);
      setIsEditingBio(false);
      setBioMessage("Bio saved.");
    } catch {
      toast.error("Could not save your bio.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleServiceSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (!authToken) {
      toast.error("Log in to create a listing.");
      return;
    }

    setServiceMessage("");

    try {
      const tags = parseServiceTags(serviceTags);
      const priceText = servicePrice.trim();
      const minPriceText = serviceMinPrice.trim();
      const maxPriceText = serviceMaxPrice.trim();
      const titleText = serviceTitle.trim();
      const descriptionText = serviceDescription.trim();
      const locationText = serviceLocation.trim();
      let priceMin = 0;
      let priceMax = 0;

      if (!titleText) {
        toast.error("Enter a title.");
        return;
      }

      if (titleText.length > SERVICE_TITLE_MAX_LENGTH) {
        toast.error("Keep the title to 80 characters or fewer.");
        return;
      }

      if (tags.length > SERVICE_TAG_MAX_COUNT) {
        toast.error("Use no more than 5 tags.");
        return;
      }

      if (
        tags.some(
          (tag) => tag.length > SERVICE_TAG_MAX_LENGTH
        )
      ) {
        toast.error("Keep each tag to 50 characters or fewer.");
        return;
      }

      if (!serviceCategory) {
        toast.error("Choose a category.");
        return;
      }

      if (servicePricingType === "flat") {
        priceMin = Number(priceText);
        priceMax = priceMin;

        if (!priceText) {
          toast.error("Enter a price.");
          return;
        }

        if (
          !isPriceInputValue(priceText) ||
          !Number.isFinite(priceMin)
        ) {
          toast.error("Enter a valid price.");
          return;
        }
      } else {
        priceMin = Number(minPriceText);
        priceMax = Number(maxPriceText);

        if (!minPriceText || !maxPriceText) {
          toast.error(
            "Enter both a minimum and maximum price."
          );
          return;
        }

        if (
          !isPriceInputValue(minPriceText) ||
          !isPriceInputValue(maxPriceText) ||
          !Number.isFinite(priceMin) ||
          !Number.isFinite(priceMax)
        ) {
          toast.error("Enter valid prices.");
          return;
        }

        if (priceMax < priceMin) {
          toast.error(
            "Price max must be greater than or equal to price min."
          );
          return;
        }
      }

      if (!descriptionText) {
        toast.error("Enter a description.");
        return;
      }

      if (
        descriptionText.length >
        SERVICE_DESCRIPTION_MAX_LENGTH
      ) {
        toast.error(
          "Keep the description to 1000 characters or fewer."
        );
        return;
      }

      if (!locationText) {
        toast.error("Enter a location.");
        return;
      }

      const requestBody: Record<string, unknown> = {
        title: titleText,
        category: serviceCategory,
        priceMin: priceMin,
        priceMax: priceMax,
        priceUnit:
          servicePriceUnit === NO_PRICE_UNIT_VALUE
            ? ""
            : normalizePriceUnit(servicePriceUnit),
        description: descriptionText,
        location: locationText,
        tags: tags
      };

      setIsCreatingService(true);

      const response = await fetch(
        isEditingService && editingServiceId
          ? API_ENDPOINTS.services.service(editingServiceId)
          : API_ENDPOINTS.services.services,
        {
          method: isEditingService ? "PUT" : "POST",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify(requestBody)
        }
      );

      if (!response.ok) {
        const message = (await response.text()).trim();
        throw new Error(
          message ||
            (isEditingService
              ? "Could not save listing."
              : "Could not create listing.")
        );
      }

      const data = (await response.json()) as ApiService;
      const [savedService] = normalizeServices([data]);

      if (savedService) {
        setProfile((currentProfile) => ({
          ...currentProfile,
          services: isEditingService
            ? currentProfile.services.map((service) =>
                service.id === editingServiceId
                  ? savedService
                  : service
              )
            : [...currentProfile.services, savedService]
        }));
      }

      resetServiceForm();
      setServiceMessage(
        isEditingService
          ? "Listing updated."
          : "Listing created."
      );
      setIsServiceFormOpen(false);
    } catch (serviceError) {
      toast.error(
        serviceError instanceof Error
          ? serviceError.message
          : isEditingService
            ? "Could not save listing."
            : "Could not create listing."
      );
    } finally {
      setIsCreatingService(false);
    }
  }

  function resetServiceForm() {
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
    setEditingServiceId(null);
  }

  function handleCancelServiceForm() {
    resetServiceForm();
    setIsServiceFormOpen(false);
  }

  async function handleConnectStripe() {
    if (!authToken) return;
    setIsConnecting(true);
    try {
      const response = await fetch(API_ENDPOINTS.payments.connect, {
        method: "POST",
        headers: { Authorization: `Bearer ${authToken}` }
      });
      if (!response.ok) {
        toast.error("Could not start Stripe onboarding.");
        return;
      }
      const data = (await response.json()) as { onboardingUrl: string };
      window.location.href = data.onboardingUrl;
    } catch {
      toast.error("Could not start Stripe onboarding.");
    } finally {
      setIsConnecting(false);
    }
  }

  function handleEditService(service: ServiceListing) {
    const priceMin = service.priceMin.trim();
    const priceMax = service.priceMax.trim();
    const isFlatPrice =
      !priceMin || !priceMax || priceMin === priceMax;

    setServiceTitle(service.title);
    setServiceCategory(service.category);
    setServiceDescription(service.description);
    setServicePricingType(isFlatPrice ? "flat" : "range");
    setServicePrice(isFlatPrice ? priceMin || priceMax : "");
    setServiceMinPrice(isFlatPrice ? "" : priceMin);
    setServiceMaxPrice(isFlatPrice ? "" : priceMax);
    setServicePriceUnit(normalizePriceUnit(service.priceUnit));
    setServiceLocation(service.location);
    setServiceTags(service.tags.join(", "));
    setServiceMessage("");
    setEditingServiceId(service.id);
    setIsServiceFormOpen(true);
  }

  async function handleDeleteService(service: ServiceListing) {
    if (!authToken) {
      toast.error("Log in to take down a listing.");
      return;
    }

    setDeletingServiceId(service.id);
    setServiceMessage("");

    try {
      const response = await fetch(
        API_ENDPOINTS.services.service(service.id),
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${authToken}`
          }
        }
      );

      if (!response.ok) {
        const message = (await response.text()).trim();
        throw new Error(
          message || "Could not take down listing."
        );
      }

      setProfile((currentProfile) => ({
        ...currentProfile,
        services: currentProfile.services.filter(
          (currentService) => currentService.id !== service.id
        )
      }));

      if (editingServiceId === service.id) {
        resetServiceForm();
        setIsServiceFormOpen(false);
      }

      setServicePendingDeletion(null);
      setServiceMessage("Listing taken down.");
    } catch (deleteError) {
      toast.error(
        deleteError instanceof Error
          ? deleteError.message
          : "Could not take down listing."
      );
    } finally {
      setDeletingServiceId(null);
    }
  }

  const displayName =
    `${profile.firstName} ${profile.lastName}`.trim() ||
    "Profile";
  const isDeletingPendingService =
    servicePendingDeletion !== null &&
    deletingServiceId === servicePendingDeletion.id;

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
    <>
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
              className={`status-message ${error ? "form-error" : "form-success"}`}>
              {error || bioMessage}
            </p>
          )}
          {isEditingBio ? (
            <form
              aria-label="Edit profile bio"
              onSubmit={handleBioSubmit}
              className="bio-form">
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
                className="bio-textarea"
              />
              <div className="bio-form-actions">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="primary-button">
                  {isSaving ? "Saving..." : "Save Bio"}
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => {
                    setBioDraft(profile.bio);
                    setBioMessage("");
                    setError("");
                    setIsEditingBio(false);
                  }}>
                  Cancel
                </button>
              </div>
            </form>
          ) : null}
        </section>

        <section
          className="profile-section"
          aria-label="Payments">
          <div className="section-heading">
            <div>
              <h2>Payments</h2>
              <p>Connect Stripe to receive payments for your services.</p>
            </div>
            {connectStatus && !connectStatus.chargesEnabled && (
              <button
                type="button"
                className="section-action-button"
                disabled={isConnecting}
                onClick={() => void handleConnectStripe()}>
                {isConnecting
                  ? "Redirecting..."
                  : connectStatus.accountId
                  ? "Continue Setup"
                  : "Connect Stripe"}
              </button>
            )}
          </div>
          {connectStatus?.chargesEnabled ? (
            <p className="connect-status connect-status--active">
              Stripe connected. Payments are active.
            </p>
          ) : connectStatus?.detailsSubmitted ? (
            <p className="connect-status">
              Stripe setup in progress. Payments will be enabled once verification is complete.
            </p>
          ) : (
            <p className="empty-state">
              Connect a Stripe account to receive payments from customers.
            </p>
          )}
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
                  resetServiceForm();
                  setServiceMessage("");
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
              aria-label={
                isEditingService
                  ? "Edit service listing"
                  : "Create service listing"
              }
              onSubmit={handleServiceSubmit}>
              <div className="service-form-header">
                <h3>
                  {isEditingService
                    ? "Edit Service"
                    : "Create Service"}
                </h3>
              </div>
              <div className="service-form-grid">
                <label>
                  <span>Title</span>
                  <input
                    value={serviceTitle}
                    onChange={(event) => {
                      setServiceTitle(event.target.value);
                      setServiceMessage("");
                    }}
                    maxLength={SERVICE_TITLE_MAX_LENGTH}
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
                      setServiceMessage("");
                    }}
                    required>
                    <option value="" disabled hidden>
                      Select a category
                    </option>
                    {SERVICE_CATEGORY_OPTIONS.map(
                      (category) => (
                        <option
                          key={category.value}
                          value={category.value}>
                          {category.label}
                        </option>
                      )
                    )}
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
                        servicePricingType === "flat"
                          ? "active"
                          : ""
                      }
                      aria-pressed={
                        servicePricingType === "flat"
                      }
                      onClick={() => {
                        setServicePricingType("flat");
                        setServiceMessage("");
                      }}>
                      Flat Price
                    </button>
                    <button
                      type="button"
                      className={
                        servicePricingType === "range"
                          ? "active"
                          : ""
                      }
                      aria-pressed={
                        servicePricingType === "range"
                      }
                      onClick={() => {
                        setServicePricingType("range");
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
                          const nextPrice = event.target.value;

                          if (!isPriceInputValue(nextPrice)) {
                            return;
                          }

                          setServicePrice(nextPrice);
                          setServiceMessage("");
                        }}
                        inputMode="decimal"
                        pattern="[0-9]*[.]?[0-9]{0,2}"
                        type="text"
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
                            const nextPrice = event.target.value;

                            if (!isPriceInputValue(nextPrice)) {
                              return;
                            }

                            setServiceMinPrice(nextPrice);
                            setServiceMessage("");
                          }}
                          inputMode="decimal"
                          pattern="[0-9]*[.]?[0-9]{0,2}"
                          type="text"
                          placeholder="$"
                          required
                        />
                      </label>

                      <label>
                        <span>Price Max</span>
                        <input
                          value={serviceMaxPrice}
                          onChange={(event) => {
                            const nextPrice = event.target.value;

                            if (!isPriceInputValue(nextPrice)) {
                              return;
                            }

                            setServiceMaxPrice(nextPrice);
                            setServiceMessage("");
                          }}
                          inputMode="decimal"
                          pattern="[0-9]*[.]?[0-9]{0,2}"
                          type="text"
                          placeholder="$$$"
                          required
                        />
                      </label>
                    </>
                  )}

                  <label>
                    <span>Price Unit (optional)</span>
                    <select
                      value={servicePriceUnit}
                      onChange={(event) => {
                        setServicePriceUnit(event.target.value);
                        setServiceMessage("");
                      }}>
                      <option value="" disabled hidden>
                        Select a price unit
                      </option>
                      <option value={NO_PRICE_UNIT_VALUE}>N/A</option>
                      {hasCustomPriceUnit && (
                        <option value={servicePriceUnit}>
                          {formatPriceUnit(servicePriceUnit)}
                        </option>
                      )}
                      {PRICE_UNIT_OPTIONS.map((unit) => (
                        <option key={unit.value} value={unit.value}>
                          {unit.label}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
              </div>

              <label>
                <span>Description</span>
                <textarea
                  value={serviceDescription}
                  onChange={(event) => {
                    setServiceDescription(event.target.value);
                    setServiceMessage("");
                  }}
                  maxLength={SERVICE_DESCRIPTION_MAX_LENGTH}
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
                    setServiceMessage("");
                  }}
                  placeholder="e.g. Python, Data Science, Algorithms"
                  rows={2}
                />
              </label>

              <div className="service-form-actions">
                <button
                  type="submit"
                  disabled={isCreatingService}>
                  {isCreatingService
                    ? isEditingService
                      ? "Saving..."
                      : "Creating..."
                    : isEditingService
                      ? "Save Changes"
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
                      <div
                        className="listing-card-actions"
                        role="group"
                        aria-label={`${service.title} actions`}>
                        <button
                          type="button"
                          onClick={() =>
                            handleEditService(service)
                          }>
                          Edit
                        </button>
                        <button
                          type="button"
                          className="danger-button"
                          disabled={
                            deletingServiceId === service.id
                          }
                          onClick={() =>
                            setServicePendingDeletion(service)
                          }>
                          {deletingServiceId === service.id
                            ? "Taking down..."
                            : "Take Down"}
                        </button>
                      </div>
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
                      <span
                        aria-hidden="true"
                        className="listing-location-pin">
                        <svg
                          viewBox="0 0 24 24"
                          focusable="false">
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

      {servicePendingDeletion && (
        <div
          className="profile-modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (
              event.target === event.currentTarget &&
              !isDeletingPendingService
            ) {
              setServicePendingDeletion(null);
            }
          }}>
          <section
            className="profile-confirm-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="take-down-title">
            <h2 id="take-down-title">Take Down Service</h2>
            <p>
              This will remove{" "}
              <strong>
                {servicePendingDeletion.title || "this service"}
              </strong>{" "}
              from your profile and campus services.
            </p>
            <div className="profile-confirm-actions">
              <button
                type="button"
                className="profile-confirm-danger"
                disabled={isDeletingPendingService}
                onClick={() =>
                  void handleDeleteService(
                    servicePendingDeletion
                  )
                }>
                {isDeletingPendingService
                  ? "Taking down..."
                  : "Take Down"}
              </button>
              <button
                type="button"
                className="profile-confirm-cancel"
                disabled={isDeletingPendingService}
                onClick={() => setServicePendingDeletion(null)}>
                Cancel
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

export default ProfilePage;
