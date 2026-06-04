import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import "../styles/ProfilePage.css";
import { API_ENDPOINTS } from "../utils/api";
import {
  normalizePriceUnit,
  PRICE_UNIT_OPTIONS
} from "../utils/pricing";
import { toast } from "react-toastify";
import type {
  ApiBooking,
  ApiUserProfile,
  ApiService
} from "../utils/types";
import { formatProviderRating } from "../utils/serviceFormatting";
import { useNavigate } from "react-router-dom";
import {
  NO_PRICE_UNIT_VALUE,
  REVIEWABLE_BOOKING_STATUS,
  REVIEW_MAX_LENGTH,
  SERVICE_DESCRIPTION_MAX_LENGTH,
  SERVICE_TAG_MAX_COUNT,
  SERVICE_TAG_MAX_LENGTH,
  SERVICE_TITLE_MAX_LENGTH,
  TOKEN_STORAGE_KEY,
  USER_ID_KEY,
  emptyProfile
} from "./profile/constants";
import type {
  ConnectStatus,
  CustomerBooking,
  ReviewDraft,
  ServiceListing,
  ServicePricingType,
  UserProfile
} from "./profile/types";
import {
  isPriceInputValue,
  normalizeBookings,
  normalizeProfile,
  normalizeServices,
  parseServiceTags
} from "./profile/utils";
import { BioSection } from "./profile/BioSection";
import {
  DeleteServiceModal,
  ReviewModal
} from "./profile/ProfileModals";
import { PaymentsSection } from "./profile/PaymentsSection";
import { ProfileHeader } from "./profile/ProfileHeader";
import { ServicesSection } from "./profile/ServicesSection";
import { ReviewsModal } from "../components/ReviewsModal";

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
  const [customerBookings, setCustomerBookings] = useState<
    CustomerBooking[]
  >([]);
  
  const [reviewDrafts, setReviewDrafts] = useState<
    Record<string, ReviewDraft>
  >({});
  const [submittingReviewId, setSubmittingReviewId] = useState<
    string | null
  >(null);
  const [reviewingBookingId, setReviewingBookingId] = useState<
    string | null
  >(null);
  const [serviceTitle, setServiceTitle] = useState("");
  const [serviceCategory, setServiceCategory] = useState("");
  const [servicePricingType, setServicePricingType] =
    useState<ServicePricingType>("flat");
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
  const [servicePostingType, setServicePostingType] =
    useState("continuous");
  const navigate = useNavigate();
  const [connectStatus, setConnectStatus] =
    useState<ConnectStatus | null>(null);
  const [isConnecting, setIsConnecting] = useState(false);
  const [editingServiceId, setEditingServiceId] = useState<
    string | null
  >(null);
  const [deletingServiceId, setDeletingServiceId] = useState<
    string | null
  >(null);
  const [servicePendingDeletion, setServicePendingDeletion] =
    useState<ServiceListing | null>(null);
  const [showOwnReviews, setShowOwnReviews] = useState(false);

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

        if (!data.verified) {
          toast.warning(
            "Please verify your account before proceeding."
          );
          navigate("/verify");
        }
        const nextProfile = normalizeProfile(data);

        const connectResponse = await fetch(
          API_ENDPOINTS.payments.connectStatus,
          {
            headers: { Authorization: `Bearer ${authToken}` }
          }
        );

        const bookingsResponse = await fetch(
          API_ENDPOINTS.bookings.mine,
          {
            headers: { Authorization: `Bearer ${authToken}` }
          }
        );

        const nextBookings = bookingsResponse.ok
          ? normalizeBookings(
              (await bookingsResponse.json()) as ApiBooking[]
            )
          : [];

        if (!bookingsResponse.ok) {
          toast.error("Could not load your bookings.");
        }

        if (isMounted) {
          setProfile(nextProfile);
          setCustomerBookings(nextBookings);
          setBioDraft(nextProfile.bio);
          setIsEditingBio(false);
          if (connectResponse.ok) {
            setConnectStatus(
              (await connectResponse.json()) as ConnectStatus
            );
          }
        }
      } catch {
        if (isMounted) {
          toast.error("Could not load your profile.");
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
          setIsLoadingBookings(false);
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
        toast.error(
          "Keep the title to 80 characters or fewer."
        );
        return;
      }

      if (tags.length > SERVICE_TAG_MAX_COUNT) {
        toast.error("Use no more than 5 tags.");
        return;
      }

      if (
        tags.some((tag) => tag.length > SERVICE_TAG_MAX_LENGTH)
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
        descriptionText.length > SERVICE_DESCRIPTION_MAX_LENGTH
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
        tags: tags,
        postingType: servicePostingType
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
    setServicePostingType("continuous");
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
      const response = await fetch(
        API_ENDPOINTS.payments.connect,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${authToken}` }
        }
      );
      if (!response.ok) {
        toast.error("Could not start Stripe onboarding.");
        return;
      }
      const data = (await response.json()) as {
        onboardingUrl: string;
      };
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

  function updateReviewDraft(
    bookingId: string,
    nextDraft: Partial<ReviewDraft>
  ) {
    setReviewDrafts((currentDrafts) => {
      const currentDraft = currentDrafts[bookingId] ?? {
        rating: "5",
        review: ""
      };

      return {
        ...currentDrafts,
        [bookingId]: {
          ...currentDraft,
          ...nextDraft
        }
      };
    });
  }

  async function handleReviewSubmit(
    event: FormEvent<HTMLFormElement>,
    booking: CustomerBooking
  ): Promise<boolean> {
    event.preventDefault();

    if (!authToken) {
      toast.error("Log in to leave a review.");
      return false;
    }

    if (booking.status !== REVIEWABLE_BOOKING_STATUS) {
      toast.error("You can only review completed bookings.");
      return false;
    }

    const draft = reviewDrafts[booking.id] ?? {
      rating: "5",
      review: ""
    };
    const rating = Number(draft.rating);
    const review = draft.review.trim();

    if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
      toast.error("Choose a rating from 1 to 5.");
      return false;
    }

    if (!review) {
      toast.error("Write a review before submitting.");
      return false;
    }

    if (review.length > REVIEW_MAX_LENGTH) {
      toast.error(
        "Keep the review to 1000 characters or fewer."
      );
      return false;
    }

    setSubmittingReviewId(booking.id);

    try {
      const response = await fetch(
        API_ENDPOINTS.bookings.review(booking.id),
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ rating, review })
        }
      );

      if (!response.ok) {
        const message = (await response.text()).trim();
        throw new Error(message || "Could not submit review.");
      }

      const [updatedBooking] = normalizeBookings([
        (await response.json()) as ApiBooking
      ]);

      if (updatedBooking) {
        setCustomerBookings((currentBookings) =>
          currentBookings.map((currentBooking) =>
            currentBooking.id === updatedBooking.id
              ? updatedBooking
              : currentBooking
          )
        );
      }

      setReviewDrafts((currentDrafts) => {
        const nextDrafts = { ...currentDrafts };
        delete nextDrafts[booking.id];
        return nextDrafts;
      });
      toast.success("Review submitted.");
      return true;
    } catch (reviewError) {
      toast.error(
        reviewError instanceof Error
          ? reviewError.message
          : "Could not submit review."
      );
      return false;
    } finally {
      setSubmittingReviewId(null);
    }
  }

  const displayName =
    `${profile.firstName} ${profile.lastName}`.trim() ||
    "Profile";
  const ratingText = formatProviderRating(
    profile.averageRating,
    profile.reviewCount
  );
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
        <ProfileHeader
          displayName={displayName}
          profile={profile}
          ratingText={ratingText}
          onReviewsClick={() => setShowOwnReviews(true)}
        />

        <BioSection
          profile={profile}
          bioDraft={bioDraft}
          bioMessage={bioMessage}
          error={error}
          isEditingBio={isEditingBio}
          isSaving={isSaving}
          onBioDraftChange={(value) => {
            setBioDraft(value);
            setBioMessage("");
            setError("");
          }}
          onCancel={() => {
            setBioDraft(profile.bio);
            setBioMessage("");
            setError("");
            setIsEditingBio(false);
          }}
          onEdit={() => {
            setBioDraft(profile.bio);
            setBioMessage("");
            setError("");
            setIsEditingBio(true);
          }}
          onSubmit={handleBioSubmit}
        />

        <PaymentsSection
          connectStatus={connectStatus}
          isConnecting={isConnecting}
          onConnectStripe={() => void handleConnectStripe()}
        />

        <ServicesSection
          deletingServiceId={deletingServiceId}
          hasCustomPriceUnit={hasCustomPriceUnit}
          isCreatingService={isCreatingService}
          isEditingService={isEditingService}
          isServiceFormOpen={isServiceFormOpen}
          serviceCategory={serviceCategory}
          serviceDescription={serviceDescription}
          serviceLocation={serviceLocation}
          serviceMaxPrice={serviceMaxPrice}
          serviceMessage={serviceMessage}
          serviceMinPrice={serviceMinPrice}
          servicePrice={servicePrice}
          servicePriceUnit={servicePriceUnit}
          servicePricingType={servicePricingType}
          servicePostingType={servicePostingType}
          services={profile.services}
          serviceTags={serviceTags}
          serviceTitle={serviceTitle}
          onCancelServiceForm={handleCancelServiceForm}
          onEditService={handleEditService}
          onOpenCreateService={() => {
            resetServiceForm();
            setServiceMessage("");
            setIsServiceFormOpen(true);
          }}
          onRequestDeleteService={(service) =>
            setServicePendingDeletion(service)
          }
          onServiceCategoryChange={(value) => {
            setServiceCategory(value);
            setServiceMessage("");
          }}
          onServiceDescriptionChange={(value) => {
            setServiceDescription(value);
            setServiceMessage("");
          }}
          onServiceLocationChange={(value) => {
            setServiceLocation(value);
            setServiceMessage("");
          }}
          onServiceMaxPriceChange={(value) => {
            setServiceMaxPrice(value);
            setServiceMessage("");
          }}
          onServiceMinPriceChange={(value) => {
            setServiceMinPrice(value);
            setServiceMessage("");
          }}
          onServicePriceChange={(value) => {
            setServicePrice(value);
            setServiceMessage("");
          }}
          onServicePriceUnitChange={(value) => {
            setServicePriceUnit(value);
            setServiceMessage("");
          }}
          onServicePricingTypeChange={(value) => {
            setServicePricingType(value);
            setServiceMessage("");
          }}
          onServicePostingTypeChange={(value) => {
            setServicePostingType(value);
            setServiceMessage("");
          }}
          onServiceTagsChange={(value) => {
            setServiceTags(value);
            setServiceMessage("");
          }}
          onServiceTitleChange={(value) => {
            setServiceTitle(value);
            setServiceMessage("");
          }}
          onSubmit={handleServiceSubmit}
        />
      </main>

      {servicePendingDeletion && (
        <DeleteServiceModal
          isDeleting={isDeletingPendingService}
          service={servicePendingDeletion}
          onCancel={() => setServicePendingDeletion(null)}
          onConfirm={(service) =>
            void handleDeleteService(service)
          }
        />
      )}

      {reviewingBookingId && (
        <ReviewModal
          bookings={customerBookings}
          reviewingBookingId={reviewingBookingId}
          reviewDrafts={reviewDrafts}
          submittingReviewId={submittingReviewId}
          onClose={() => setReviewingBookingId(null)}
          onSubmit={handleReviewSubmit}
          onUpdateDraft={updateReviewDraft}
        />
      )}

      {showOwnReviews && (
        <ReviewsModal
          providerId={localStorage.getItem(USER_ID_KEY) || ""}
          providerName={displayName}
          onClose={() => setShowOwnReviews(false)}
        />
      )}
    </>
  );
}

export default ProfilePage;
