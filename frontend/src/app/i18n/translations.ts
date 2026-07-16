import { commonFr, commonAr } from './common';
import { statusFr, statusAr } from './status';
import { materialFr, materialAr } from './material';
import { authFr, authAr } from './auth';
import { requestsFr, requestsAr } from './requests';
import { recyclersFr, recyclersAr } from './recyclers';
import { municipalityFr, municipalityAr } from './municipality';
import { adminFr, adminAr } from './admin';

/**
 * Each namespace file owns one top-level key (common, status, material, auth, ...) so merging
 * them is a plain shallow spread - no key collisions to resolve, and each namespace can be
 * edited independently.
 */
export const translations = {
  fr: { ...commonFr, ...statusFr, ...materialFr, ...authFr, ...requestsFr, ...recyclersFr, ...municipalityFr, ...adminFr },
  ar: { ...commonAr, ...statusAr, ...materialAr, ...authAr, ...requestsAr, ...recyclersAr, ...municipalityAr, ...adminAr }
};
