export interface AdminUserDto {
  id: string;
  email: string;
  role: string;
  fullName: string;
  phone: string | null;
  active: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
}
