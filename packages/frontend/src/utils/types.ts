
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