export type Role = 'HOUSEHOLD_SME' | 'RECYCLER' | 'MUNICIPALITY' | 'ADMIN';

export interface RegisterRequest {
  email: string;
  password: string;
  role: Role;
  fullName: string;
  phone?: string;
  preferredLang?: 'fr' | 'ar';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  role: Role;
  preferredLang?: 'fr' | 'ar';
}
