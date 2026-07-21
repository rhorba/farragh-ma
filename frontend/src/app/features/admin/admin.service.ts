import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RequestResponseDto } from '../requests/request.models';
import {
  AdminActionLogDto,
  AdminUserDto,
  AnalyticsGranularity,
  PageResponse,
  RequestsAnalyticsSummaryDto,
  RequestsTimeSeriesPointDto
} from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  searchUsers(email: string | null, role: string | null): Observable<PageResponse<AdminUserDto>> {
    return this.http.get<PageResponse<AdminUserDto>>(`${this.baseUrl}/users`, { params: buildParams({ email, role }) });
  }

  searchRequests(
    status: string | null,
    createdFrom: string | null = null,
    createdTo: string | null = null
  ): Observable<PageResponse<RequestResponseDto>> {
    return this.http.get<PageResponse<RequestResponseDto>>(`${this.baseUrl}/requests`, {
      params: buildParams({ status, createdFrom, createdTo })
    });
  }

  getRequestsSummary(from: string | null, to: string | null): Observable<RequestsAnalyticsSummaryDto> {
    return this.http.get<RequestsAnalyticsSummaryDto>(`${this.baseUrl}/analytics/requests/summary`, {
      params: buildParams({ from, to })
    });
  }

  getRequestsTimeSeries(
    from: string | null,
    to: string | null,
    granularity: AnalyticsGranularity
  ): Observable<RequestsTimeSeriesPointDto[]> {
    return this.http.get<RequestsTimeSeriesPointDto[]>(`${this.baseUrl}/analytics/requests/timeseries`, {
      params: buildParams({ from, to, granularity })
    });
  }

  exportRequestsTimeSeriesCsv(from: string | null, to: string | null, granularity: AnalyticsGranularity): Observable<string> {
    return this.http.get(`${this.baseUrl}/analytics/requests/export`, {
      params: buildParams({ from, to, granularity }),
      responseType: 'text'
    });
  }

  deactivateUser(id: string): Observable<AdminUserDto> {
    return this.http.post<AdminUserDto>(`${this.baseUrl}/users/${id}/deactivate`, {});
  }

  reactivateUser(id: string): Observable<AdminUserDto> {
    return this.http.post<AdminUserDto>(`${this.baseUrl}/users/${id}/reactivate`, {});
  }

  getActionLog(): Observable<PageResponse<AdminActionLogDto>> {
    return this.http.get<PageResponse<AdminActionLogDto>>(`${this.baseUrl}/action-log`);
  }
}

function buildParams(values: Record<string, string | null>): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value) {
      params = params.set(key, value);
    }
  }
  return params;
}
