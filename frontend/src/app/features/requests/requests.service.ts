import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateRequestDto, PaymentResponseDto, RequestResponseDto } from './request.models';

@Injectable({ providedIn: 'root' })
export class RequestsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/requests`;

  create(dto: CreateRequestDto): Observable<RequestResponseDto> {
    return this.http.post<RequestResponseDto>(this.baseUrl, dto);
  }

  listMine(): Observable<RequestResponseDto[]> {
    return this.http.get<RequestResponseDto[]>(this.baseUrl);
  }

  getMine(id: string): Observable<RequestResponseDto> {
    return this.http.get<RequestResponseDto>(`${this.baseUrl}/${id}`);
  }

  cancel(id: string): Observable<RequestResponseDto> {
    return this.http.post<RequestResponseDto>(`${this.baseUrl}/${id}/cancel`, {});
  }

  pay(id: string): Observable<PaymentResponseDto> {
    return this.http.post<PaymentResponseDto>(`${this.baseUrl}/${id}/payment`, {});
  }
}
