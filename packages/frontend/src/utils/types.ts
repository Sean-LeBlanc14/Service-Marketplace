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
  | "CANCELLED"
  | "REJECTED";

export type MessageType =
  | "TEXT"
  | "PRICE_OFFER"
  | "PRICE_ACCEPTED"
  | "PRICE_REJECTED";

export type NotificationType =
  | "NEW_MESSAGE"
  | "PRICE_OFFER_RECEIVED"
  | "PRICE_OFFER_ACCEPTED"
  | "PRICE_OFFER_REJECTED"
  | "BOOKING_REQUESTED"
  | "BOOKING_CONFIRMED"
  | "BOOKING_CANCELLED"
  | "BOOKING_REJECTED"
  | "REVIEW_RECEIVED";

export interface ApiConversation {
  id: string;
  serviceId: string;
  serviceTitle: string;
  customerId: string;
  customerName: string;
  providerId: string;
  providerName: string;
  lastMessagePreview: string | null;
  lastMessageAt: string | null;
  unreadCount: number;
  createdAt: string;
}

export interface ApiMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  type: MessageType;
  content: string | null;
  offeredPrice: number | string | null;
  offerResponded: boolean;
  read: boolean;
  createdAt: string;
}

export interface ApiNotification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  referenceId: string | null;
  read: boolean;
  createdAt: string;
}

export interface ApiUnreadCounts {
  messages: number;
  notifications: number;
}

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
