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
    profile: `${API_ROUTE}/users/me`
  },
  services: {
    services: `${API_ROUTE}/services`,
    service: (serviceId: string) =>
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
      `${API_ROUTE}/bookings/${encodeURIComponent(bookingId)}/review`
  }
};
