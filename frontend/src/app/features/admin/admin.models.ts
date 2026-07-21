export interface AdminUserDto {
  id: string;
  email: string;
  role: string;
  fullName: string;
  phone: string | null;
  active: boolean;
  createdAt: string;
}

export interface AdminActionLogDto {
  id: string;
  adminEmail: string;
  targetEmail: string;
  action: 'DEACTIVATE' | 'REACTIVATE';
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
}
