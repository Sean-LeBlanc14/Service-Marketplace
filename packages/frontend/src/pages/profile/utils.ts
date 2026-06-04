import type {
  ApiBooking,
  ApiService,
  ApiUserProfile
} from "../../utils/types";
import { normalizePriceUnit } from "../../utils/pricing";
import {
  normalizeRatingValue,
  normalizeReviewCount
} from "../../utils/serviceFormatting";
import { SERVICE_CATEGORY_OPTIONS } from "./constants";
import type {
  CustomerBooking,
  ServiceListing,
  UserProfile
} from "./types";

const PRICE_INPUT_PATTERN = /^\d*(?:\.\d{0,2})?$/;

export function cleanText(value?: string) {
  return value?.trim() ?? "";
}

export function cleanPriceValue(
  value?: number | string | null
) {
  return value == null ? "" : String(value).trim();
}

export function isPriceInputValue(value: string) {
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

export function parseServiceTags(value: string) {
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

export function formatPrice(service: ServiceListing) {
  const minPrice = service.priceMin || service.priceMax;
  const maxPrice = service.priceMax || service.priceMin;
  const displayPrice =
    minPrice && maxPrice && minPrice !== maxPrice
      ? `${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}`
      : formatCurrency(minPrice || maxPrice);
  const priceUnit = normalizePriceUnit(service.priceUnit);

  return priceUnit
    ? `${displayPrice}/${priceUnit}`
    : displayPrice;
}

export function formatBookingPrice(booking: CustomerBooking) {
  const displayPrice = formatCurrency(booking.agreedPrice);
  const priceUnit = normalizePriceUnit(booking.priceUnit);

  return priceUnit
    ? `${displayPrice}/${priceUnit}`
    : displayPrice;
}

export function formatDateTime(value: string) {
  if (!value) {
    return "Not scheduled";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Not scheduled";
  }

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

export function formatBookingStatus(status: string) {
  return status
    .toLocaleLowerCase()
    .split("_")
    .map((word) =>
      word
        ? `${word.charAt(0).toLocaleUpperCase()}${word.slice(1)}`
        : word
    )
    .join(" ");
}

export function formatBookingStatusClass(status: string) {
  return status.toLocaleLowerCase().replace(/_/g, "-");
}

export function formatCategory(category: string) {
  return (
    SERVICE_CATEGORY_OPTIONS.find(
      (option) => option.value === category
    )?.label || category
  );
}

export function normalizeServices(
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

export function normalizeBookings(
  bookings: ApiBooking[] | undefined
): CustomerBooking[] {
  if (!Array.isArray(bookings)) {
    return [];
  }

  return bookings.map((booking, index) => ({
    id: cleanText(booking.id) || `booking-${index}`,
    serviceId: cleanText(booking.serviceId),
    serviceTitle:
      cleanText(booking.serviceTitle) || "Booked service",
    customerName: cleanText(booking.customerName) || "You",
    providerName: cleanText(booking.providerName),
    reviewerName:
      cleanText(booking.reviewerName) ||
      cleanText(booking.customerName) ||
      "You",
    agreedPrice: cleanPriceValue(booking.agreedPrice),
    priceUnit: cleanText(booking.priceUnit ?? undefined),
    scheduledAt: cleanText(booking.scheduledAt ?? undefined),
    status: cleanText(booking.status ?? undefined),
    rating:
      typeof booking.rating === "number"
        ? booking.rating
        : null,
    review: cleanText(booking.review ?? undefined),
    reviewedAt: cleanText(booking.reviewedAt ?? undefined),
    createdAt: cleanText(booking.createdAt ?? undefined)
  }));
}

export function normalizeProfile(
  profile: ApiUserProfile
): UserProfile {
  return {
    email: cleanText(profile.email),
    firstName: cleanText(profile.firstName),
    lastName: cleanText(profile.lastName),
    major: cleanText(profile.major),
    campus: cleanText(profile.campus),
    bio: cleanText(profile.bio),
    averageRating: normalizeRatingValue(profile.averageRating),
    reviewCount: normalizeReviewCount(profile.reviewCount),
    services: normalizeServices(profile.services)
  };
}
