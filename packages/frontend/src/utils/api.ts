const API_ROUTE = import.meta.env.VITE_API_BASE_ROUTE;

export const API_ENDPOINTS = {
  auth: {
    login: `${API_ROUTE}/auth/login`,
    signup: `${API_ROUTE}/auth/register`,
    logout: `${API_ROUTE}/auth/logout`
  },
  verification: {
    verify: `${API_ROUTE}/verification/code`,
    resend: `${API_ROUTE}/verification/resend`
  },
  user: {
    profile: `${API_ROUTE}/users/me`,
    providerProfile: (userId: string) =>
      `${API_ROUTE}/users/${encodeURIComponent(userId)}/profile`,
    other: (userId: string) =>
      `${API_ROUTE}/users/profile/${encodeURIComponent(userId)}`,
    delete: `${API_ROUTE}/users/delete`,
    changePassword: `${API_ROUTE}/users/password`,
    changeMajor: `${API_ROUTE}/users/major`,
    changeCampus: `${API_ROUTE}/users/campus`
  },
  users: {
    all: `${API_ROUTE}/users`,
    getById: (userId: string) =>
      `${API_ROUTE}/users/${encodeURIComponent(userId)}`,
    suspend: (userId: string) =>
      `${API_ROUTE}/users/${encodeURIComponent(userId)}/suspend`,
    unsuspend: (userId: string) =>
      `${API_ROUTE}/users/${encodeURIComponent(userId)}/unsuspend`
  },
  services: {
    services: `${API_ROUTE}/services`,
    service: (serviceId: string) =>
      `${API_ROUTE}/services/${encodeURIComponent(serviceId)}`,
    delete: (serviceId: string) =>
      `${API_ROUTE}/services/${encodeURIComponent(serviceId)}`
  },
  payments: {
    connect: `${API_ROUTE}/payments/connect`,
    connectStatus: `${API_ROUTE}/payments/connect/status`
  },
  bookings: {
    create: `${API_ROUTE}/bookings`,
    mine: `${API_ROUTE}/bookings/me`,
    review: (bookingId: string) =>
      `${API_ROUTE}/bookings/${encodeURIComponent(bookingId)}/review`,
    confirm: (bookingId: string) =>
      `${API_ROUTE}/bookings/${encodeURIComponent(bookingId)}/confirm`,
    cancel: (bookingId: string) =>
      `${API_ROUTE}/bookings/${encodeURIComponent(bookingId)}/cancel`,
    providerReviews: (providerId: string) =>
      `${API_ROUTE}/bookings/provider/${encodeURIComponent(providerId)}/reviews`,
    reject: (bookingId: string) =>
      `${API_ROUTE}/bookings/${encodeURIComponent(bookingId)}/reject`,
    getProviderRequests: `${API_ROUTE}/bookings/provider/requests/me`,
    getProviderScheduled: `${API_ROUTE}/bookings/provider/scheduled/me`,
    getProviderCompleted: `${API_ROUTE}/bookings/provider/completed/me`
  },
  reports: {
    create: `${API_ROUTE}/reports`,
    all: `${API_ROUTE}/reports`,
    resolve: (reportId: string) =>
      `${API_ROUTE}/reports/${encodeURIComponent(reportId)}/resolve`
  },
  support: {
    bug: `${API_ROUTE}/support/bug`,
    contact: `${API_ROUTE}/support/contact`
  },
  conversations: {
    start: `${API_ROUTE}/conversations`,
    all: `${API_ROUTE}/conversations`,
    messages: (conversationId: string) =>
      `${API_ROUTE}/conversations/${encodeURIComponent(conversationId)}/messages`,
    sendMessage: (conversationId: string) =>
      `${API_ROUTE}/conversations/${encodeURIComponent(conversationId)}/messages`,
    acceptOffer: (conversationId: string, messageId: string) =>
      `${API_ROUTE}/conversations/${encodeURIComponent(conversationId)}/messages/${encodeURIComponent(messageId)}/accept`,
    rejectOffer: (conversationId: string, messageId: string) =>
      `${API_ROUTE}/conversations/${encodeURIComponent(conversationId)}/messages/${encodeURIComponent(messageId)}/reject`
  },
  notifications: {
    all: `${API_ROUTE}/notifications`,
    markRead: (notificationId: string) =>
      `${API_ROUTE}/notifications/${encodeURIComponent(notificationId)}/read`,
    markAllRead: `${API_ROUTE}/notifications/read-all`
  },
  unreadCounts: `${API_ROUTE}/unread-counts`
};
