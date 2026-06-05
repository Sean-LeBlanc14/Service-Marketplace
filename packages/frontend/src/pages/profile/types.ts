export interface ServiceListing {
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

export interface UserProfile {
  email: string;
  firstName: string;
  lastName: string;
  major: string;
  campus: string;
  bio: string;
  averageRating: number | null;
  reviewCount: number;
  services: ServiceListing[];
}

export interface CustomerBooking {
  id: string;
  serviceId: string;
  serviceTitle: string;
  customerName: string;
  providerName: string;
  reviewerName: string;
  agreedPrice: string;
  priceUnit: string;
  scheduledAt: string;
  status: string;
  rating: number | null;
  review: string;
  reviewedAt: string;
  createdAt: string;
}

export interface ReviewDraft {
  rating: string;
  review: string;
}

export interface ConnectStatus {
  accountId: string | null;
  chargesEnabled: boolean;
  detailsSubmitted: boolean;
  payoutsEnabled: boolean;
}

export type ServicePricingType = "flat" | "range";
