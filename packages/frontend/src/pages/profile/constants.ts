import type { UserProfile } from "./types";

export const TOKEN_STORAGE_KEY = "jwt_token";

export const SERVICE_CATEGORY_OPTIONS = [
  { value: "tutoring", label: "Tutoring" },
  { value: "tech help", label: "Tech Help" },
  { value: "housing", label: "Housing" },
  { value: "finance", label: "Finance" },
  { value: "food and catering", label: "Food and Catering" },
  { value: "photography", label: "Photography" }
];

export const NO_PRICE_UNIT_VALUE = "__none__";

export const SERVICE_TITLE_MAX_LENGTH = 80;
export const SERVICE_DESCRIPTION_MAX_LENGTH = 1000;
export const SERVICE_TAG_MAX_COUNT = 5;
export const SERVICE_TAG_MAX_LENGTH = 50;
export const REVIEW_MAX_LENGTH = 1000;
export const REVIEWABLE_BOOKING_STATUS = "COMPLETED";

export const emptyProfile: UserProfile = {
  email: "",
  firstName: "",
  lastName: "",
  major: "",
  campus: "",
  bio: "",
  averageRating: null,
  reviewCount: 0,
  services: []
};
