import { test, expect } from '@playwright/test';
import {
  uniqueEmail,
  registerRecycler,
  registerHousehold,
  declareRecyclerZone,
  createRequest,
  newActorContext,
  saveVideoIfRecording
} from './helpers';

test.use({ locale: 'fr-FR' });

const ZONE = { lat: 33.5731, lng: -7.5898, radiusM: 5000, materialCode: 'PLASTIC' };

test('golden path: post request -> recycler accepts -> status progression -> mock payment (FR)', async ({ browser }) => {
  const recyclerCtx = await newActorContext(browser);
  const householdCtx = await newActorContext(browser);
  const recyclerPage = await recyclerCtx.newPage();
  const householdPage = await householdCtx.newPage();

  const recyclerCreds = { email: uniqueEmail('recycler-fr'), password: 'RecyclerPass123' };
  const householdCreds = { email: uniqueEmail('household-fr'), password: 'HouseholdPass123' };

  await registerRecycler(recyclerPage, recyclerCreds, 'Recycler FR');
  await declareRecyclerZone(recyclerPage, ZONE);

  const addressText = `12 Rue de la Poste, Casablanca [${householdCreds.email}]`;
  await registerHousehold(householdPage, householdCreds, 'Household FR');
  const requestId = await createRequest(householdPage, {
    materialCode: ZONE.materialCode,
    addressText,
    lat: ZONE.lat,
    lng: ZONE.lng
  });
  await expect(householdPage.locator('.status-badge.POSTED')).toBeVisible();

  await recyclerPage.goto('/recycler/feed');
  const feedCard = recyclerPage.locator('.request-card', { hasText: addressText });
  await expect(feedCard).toBeVisible();
  await feedCard.locator('[data-testid="accept-btn"]').click();
  await expect(feedCard).toHaveCount(0);

  await recyclerPage.goto('/recycler/accepted');
  const card = recyclerPage.locator('.request-card', { hasText: addressText });
  await expect(card.locator('.status-badge.ACCEPTED')).toBeVisible();
  await card.locator('[data-testid="schedule-btn"]').click();
  await expect(card.locator('.status-badge.SCHEDULED')).toBeVisible();
  await card.locator('[data-testid="complete-btn"]').click();
  await expect(card.locator('.status-badge.COMPLETED')).toBeVisible();

  await householdPage.goto(`/requests/${requestId}`);
  await expect(householdPage.locator('.status-badge.COMPLETED')).toBeVisible();
  await householdPage.locator('[data-testid="pay-btn"]').click();
  await expect(householdPage.locator('p.paid')).toBeVisible();
  await expect(householdPage.locator('[data-testid="pay-btn"]')).toHaveCount(0);

  await recyclerCtx.close();
  await householdCtx.close();
  await saveVideoIfRecording(recyclerPage, 'v1.0-fr-recycler');
  await saveVideoIfRecording(householdPage, 'v1.0-fr-household');
});
