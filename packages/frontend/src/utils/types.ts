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

export interface ApiBooking {
  id: string;
  serviceId: string;
  serviceTitle: string;
  customerId: string;
  providerId: string;
  agreedPrice: number;
  priceUnit: string;
  scheduledAt: string;
  bookingStatus: string;
  createdAt: string;
}
