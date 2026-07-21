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

export type RequestStatusKey = 'POSTED' | 'ACCEPTED' | 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface RequestsAnalyticsSummaryDto {
  from: string;
  to: string;
  total: number;
  countsByStatus: Record<RequestStatusKey, number>;
}

export interface RequestsTimeSeriesPointDto {
  bucket: string;
  created: number;
  completed: number;
}

export type AnalyticsGranularity = 'DAY' | 'WEEK' | 'MONTH';
