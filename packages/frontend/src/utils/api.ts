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
    other: (userId: string) => `${API_ROUTE}/users/profile/${(encodeURIComponent(userId))}`,
    delete: `${API_ROUTE}/users/delete`
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
    confirm: (bookingId: string) => `${API_ROUTE}/bookings/${encodeURIComponent(bookingId)}/confirm`,
    cancel: (bookingId: string) => `${API_ROUTE}/bookings/${encodeURIComponent(bookingId)}/cancel`,
    getRequests: `${API_ROUTE}/bookings/requests`,
    getCompleted: `${API_ROUTE}/bookings/completed`,
    getScheduled: `${API_ROUTE}/bookings/scheduled`,

  },
  reports: {
    create: `${API_ROUTE}/reports`,
    all: `${API_ROUTE}/reports`,
    resolve: (reportId: string) =>
      `${API_ROUTE}/reports/${encodeURIComponent(reportId)}/resolve`
  }
};
