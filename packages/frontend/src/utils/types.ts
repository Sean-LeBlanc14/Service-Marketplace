export interface ApiService {
  id?: string;
  title?: string;
  category?: string;
  userId?: string;
  providerName?: string;
  priceMin?: number | string | null;
  priceMax?: number | string | null;
  priceUnit?: string | null;
  description?: string;
  location?: string;
  tags?: string[] | null;
}

export interface ApiUserProfile {
  email: string;
  firstName: string;
  lastName: string;
  major: string;
  campus: string;
  bio: string;
  verified: string;
  services: ApiService[];
}

export type ApiBookingStatus =
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
  agreedPrice?: number | string | null;
  priceUnit?: string | null;
  scheduledAt?: string | null;
  status?: ApiBookingStatus | string | null;
  rating?: number | null;
  review?: string | null;
  reviewedAt?: string | null;
  createdAt?: string | null;
}
