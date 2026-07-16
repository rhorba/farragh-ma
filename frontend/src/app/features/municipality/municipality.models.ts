export interface SubscribeZoneDto {
  centerLatitude?: number | null;
  centerLongitude?: number | null;
  radiusM?: number | null;
  polygon?: number[][] | null;
  confirmOverlap: boolean;
}

export interface SubscriptionResponseDto {
  id: string;
  centerLatitude?: number | null;
  centerLongitude?: number | null;
  radiusM?: number | null;
  polygon?: number[][] | null;
  active: boolean;
  createdAt: string;
}

export interface SubscribeResultDto {
  overlapWarning: boolean;
  subscription: SubscriptionResponseDto | null;
}
