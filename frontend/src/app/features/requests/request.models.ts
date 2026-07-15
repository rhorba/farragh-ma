export type RequestStatus = 'POSTED' | 'ACCEPTED' | 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED';

export interface PaymentResponseDto {
  id: string;
  pickupRequestId: string;
  amountCents: number;
  currency: string;
  provider: string;
  mode: 'MOCK' | 'LIVE';
  status: PaymentStatus;
  providerRef: string | null;
  createdAt: string;
}

export interface CreateRequestDto {
  materialTypeCode: string;
  quantityDesc?: string;
  addressText: string;
  latitude: number;
  longitude: number;
  photoUrl?: string;
}

export interface RequestResponseDto {
  id: string;
  materialTypeCode: string;
  quantityDesc?: string;
  addressText: string;
  latitude: number;
  longitude: number;
  status: RequestStatus;
  photoUrl?: string;
  createdAt: string;
  updatedAt: string;
  paymentStatus?: PaymentStatus | null;
}

export const MATERIAL_TYPES: { code: string; label: string }[] = [
  { code: 'PLASTIC', label: 'Plastique' },
  { code: 'METAL', label: 'Métal' },
  { code: 'ELECTRONIC', label: 'Électronique' },
  { code: 'ORGANIC', label: 'Organique' },
  { code: 'PAPER', label: 'Papier' },
  { code: 'GLASS', label: 'Verre' }
];
