export interface DeclareZoneDto {
  centerLatitude?: number | null;
  centerLongitude?: number | null;
  radiusM?: number | null;
  polygon?: number[][] | null;
}

export interface ZoneResponseDto {
  id: string;
  centerLatitude?: number | null;
  centerLongitude?: number | null;
  radiusM?: number | null;
  polygon?: number[][] | null;
  createdAt: string;
}

export interface DeclareMaterialsDto {
  materialTypeCodes: string[];
}

export interface MaterialsResponseDto {
  materialTypeCodes: string[];
}
