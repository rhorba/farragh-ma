import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RequestResponseDto } from '../requests/request.models';
import { AdminUserDto, PageResponse } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  searchUsers(email: string | null, role: string | null): Observable<PageResponse<AdminUserDto>> {
    return this.http.get<PageResponse<AdminUserDto>>(`${this.baseUrl}/users`, { params: buildParams({ email, role }) });
  }

  searchRequests(status: string | null): Observable<PageResponse<RequestResponseDto>> {
    return this.http.get<PageResponse<RequestResponseDto>>(`${this.baseUrl}/requests`, { params: buildParams({ status }) });
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
