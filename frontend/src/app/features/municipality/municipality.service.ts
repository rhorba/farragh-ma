import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SubscribeResultDto, SubscribeZoneDto, SubscriptionResponseDto } from './municipality.models';

@Injectable({ providedIn: 'root' })
export class MunicipalityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/municipality`;

  subscribe(dto: SubscribeZoneDto): Observable<SubscribeResultDto> {
    return this.http.post<SubscribeResultDto>(`${this.baseUrl}/subscriptions`, dto);
  }

  listMySubscriptions(): Observable<SubscriptionResponseDto[]> {
    return this.http.get<SubscriptionResponseDto[]>(`${this.baseUrl}/subscriptions`);
  }
}
