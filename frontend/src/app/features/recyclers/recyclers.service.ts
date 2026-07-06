import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RequestResponseDto } from '../requests/request.models';
import { DeclareMaterialsDto, DeclareZoneDto, MaterialsResponseDto, ZoneResponseDto } from './recycler.models';

@Injectable({ providedIn: 'root' })
export class RecyclersService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/recyclers`;

  declareZone(dto: DeclareZoneDto): Observable<ZoneResponseDto> {
    return this.http.post<ZoneResponseDto>(`${this.baseUrl}/zone`, dto);
  }

  getZone(): Observable<ZoneResponseDto> {
    return this.http.get<ZoneResponseDto>(`${this.baseUrl}/zone`);
  }

  declareMaterials(dto: DeclareMaterialsDto): Observable<MaterialsResponseDto> {
    return this.http.put<MaterialsResponseDto>(`${this.baseUrl}/materials`, dto);
  }

  getMaterials(): Observable<MaterialsResponseDto> {
    return this.http.get<MaterialsResponseDto>(`${this.baseUrl}/materials`);
  }

  getFeed(): Observable<RequestResponseDto[]> {
    return this.http.get<RequestResponseDto[]>(`${this.baseUrl}/feed`);
  }

  accept(id: string): Observable<RequestResponseDto> {
    return this.http.post<RequestResponseDto>(`${this.baseUrl}/feed/${id}/accept`, {});
  }
}
