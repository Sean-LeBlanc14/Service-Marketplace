export interface ApiService {
  id?: string;
  title?: string;
  category?: string;
  userId?: string;
  providerName?: string;
  providerAverageRating?: number | string | null;
  providerReviewCount?: number | string | null;
  priceMin?: number | string | null;
  priceMax?: number | string | null;
  priceUnit?: string | null;
  description?: string;
  location?: string;
  tags?: string[] | null;
}

export interface ApiUserProfile {
  id?: string;
  email: string;
  firstName: string;
  lastName: string;
  major: string;
  campus: string;
  bio: string;
  verified: string;
  averageRating?: number | string | null;
  reviewCount?: number | string | null;
  services: ApiService[];
}

export interface ApiProviderProfile {
  id?: string;
  firstName?: string;
  lastName?: string;
  major?: string;
  campus?: string;
  bio?: string;
  averageRating?: number | string | null;
  reviewCount?: number | string | null;
  services?: ApiService[] | null;
}

export type ApiBookingStatus =
  | "AWAITING_PROVIDER_CONFIRMATION"
  | "PENDING_PAYMENT"
  | "CONFIRMED"
  | "COMPLETED"
  | "CANCELLED";

export interface ApiBooking {
  id?: string;
  serviceId?: string;
  serviceTitle?: string;
  customerId?: string;
  providerId?: string;
  customerName?: string;
  providerName?: string;
  reviewerName?: string;
  agreedPrice?: number | string | null;
  priceUnit?: string | null;
  scheduledAt?: string | null;
  status?: ApiBookingStatus | string | null;
  rating?: number | null;
  review?: string | null;
  reviewedAt?: string | null;
  createdAt?: string | null;
}
