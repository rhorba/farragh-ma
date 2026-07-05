export type RequestStatus = 'POSTED' | 'ACCEPTED' | 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

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
}

export const MATERIAL_TYPES: { code: string; label: string }[] = [
  { code: 'PLASTIC', label: 'Plastique' },
  { code: 'METAL', label: 'Métal' },
  { code: 'ELECTRONIC', label: 'Électronique' },
  { code: 'ORGANIC', label: 'Organique' },
  { code: 'PAPER', label: 'Papier' },
  { code: 'GLASS', label: 'Verre' }
];
