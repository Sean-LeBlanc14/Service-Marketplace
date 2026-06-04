import type { Service } from "../components/ServiceCard";
import type { ApiService } from "./types";
import { normalizePriceUnit } from "./pricing";

export function cleanText(value?: string | null) {
  return value?.trim() ?? "";
}

function cleanNumberValue(value?: number | string | null) {
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

function formatPrice(service: ApiService) {
  const minPrice = cleanNumberValue(service.priceMin);
  const maxPrice = cleanNumberValue(service.priceMax);
  const displayPrice =
    minPrice && maxPrice && minPrice !== maxPrice
      ? `${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}`
      : formatCurrency(minPrice || maxPrice);
  const priceUnit = normalizePriceUnit(service.priceUnit);

  return priceUnit
    ? `${displayPrice}/${priceUnit}`
    : displayPrice;
}

export function normalizeRatingValue(
  value?: number | string | null
) {
  const rating = Number(cleanNumberValue(value));

  return Number.isFinite(rating) && rating > 0 ? rating : null;
}

export function normalizeReviewCount(
  value?: number | string | null
) {
  const count = Number(cleanNumberValue(value) || 0);

  return Number.isFinite(count) && count > 0
    ? Math.trunc(count)
    : 0;
}

export function formatProviderRating(
  averageRating: number | null,
  reviewCount: number
) {
  if (averageRating === null || reviewCount <= 0) {
    return "No ratings yet";
  }

  const reviewLabel = reviewCount === 1 ? "review" : "reviews";
  return `${averageRating.toFixed(1)} (${reviewCount} ${reviewLabel})`;
}

export function normalizeService(
  service: ApiService,
  index: number
): Service {
  const tags = Array.isArray(service.tags)
    ? service.tags.map(cleanText).filter(Boolean)
    : [];
  const id = cleanText(service.id) || `service-${index}`;
  const priceMin = Number(
    cleanNumberValue(service.priceMin) || 0
  );
  const priceMax = Number(
    cleanNumberValue(service.priceMax) || priceMin
  );
  const priceUnit = cleanText(service.priceUnit) || null;

  return {
    id,
    title: cleanText(service.title) || "Untitled service",
    category: cleanText(service.category) || "general",
    userId: cleanText(service.userId),
    providerName:
      cleanText(service.providerName) || "Service provider",
    providerAverageRating: normalizeRatingValue(
      service.providerAverageRating
    ),
    providerReviewCount: normalizeReviewCount(
      service.providerReviewCount
    ),
    price: formatPrice(service),
    priceMin,
    priceMax,
    priceUnit,
    description: cleanText(service.description),
    location: cleanText(service.location) || "Campus",
    tags
  };
}
