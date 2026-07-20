import path from 'path';
import { Browser, BrowserContext, Page, expect } from '@playwright/test';

// Story 7.4: video recording is a final-sprint-only, on-demand action (not part of every
// CI run) - gated behind RECORD_VIDEO so normal `playwright test` runs are unaffected.
const RECORD_VIDEO = !!process.env.RECORD_VIDEO;

export async function newActorContext(browser: Browser): Promise<BrowserContext> {
  return browser.newContext(RECORD_VIDEO ? { recordVideo: { dir: path.join(process.cwd(), '..', '.recordings', 'raw') } } : {});
}

export async function saveVideoIfRecording(page: Page, name: string): Promise<void> {
  if (!RECORD_VIDEO) return;
  await page.video()?.saveAs(path.join(process.cwd(), '..', '.recordings', 'raw', `${name}.webm`));
}

export interface Credentials {
  email: string;
  password: string;
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@e2e.test`;
}

export async function switchLanguage(page: Page, lang: 'fr' | 'ar'): Promise<void> {
  const label = lang === 'fr' ? 'FR' : 'عربي';
  await page.locator('.language-switcher button', { hasText: label }).click();
  await expect(page.locator('.app-shell')).toHaveAttribute('dir', lang === 'ar' ? 'rtl' : 'ltr');
}

export async function registerRecycler(
  page: Page,
  creds: Credentials,
  fullName: string
): Promise<void> {
  await page.goto('/register');
  await page.locator('mat-select[formcontrolname="role"]').click();
  await page.locator('mat-option[data-testid="role-option-RECYCLER"]').click();
  await page.locator('input[formcontrolname="fullName"]').fill(fullName);
  await page.locator('input[formcontrolname="email"]').fill(creds.email);
  await page.locator('input[formcontrolname="password"]').fill(creds.password);
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/recycler\/feed/);
}

export async function registerHousehold(
  page: Page,
  creds: Credentials,
  fullName: string
): Promise<void> {
  await page.goto('/register');
  await page.locator('mat-select[formcontrolname="role"]').click();
  await page.locator('mat-option[data-testid="role-option-HOUSEHOLD_SME"]').click();
  await page.locator('input[formcontrolname="fullName"]').fill(fullName);
  await page.locator('input[formcontrolname="email"]').fill(creds.email);
  await page.locator('input[formcontrolname="password"]').fill(creds.password);
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/requests$/);
}

export interface Zone {
  lat: number;
  lng: number;
  radiusM?: number;
  materialCode: string;
}

export async function declareRecyclerZone(page: Page, zone: Zone): Promise<void> {
  await page.goto('/recycler/profile');
  await page.locator('input[formcontrolname="centerLatitude"]').fill(String(zone.lat));
  await page.locator('input[formcontrolname="centerLongitude"]').fill(String(zone.lng));
  if (zone.radiusM) {
    await page.locator('input[formcontrolname="radiusM"]').fill(String(zone.radiusM));
  }
  await page.locator('mat-select[formcontrolname="materialTypeCodes"]').click();
  await page.locator(`mat-option[data-testid="material-option-${zone.materialCode}"]`).click();
  await page.keyboard.press('Escape');
  await page.locator('button[type="submit"]').click();
  await expect(page.locator('p.success')).toBeVisible();
}

export interface NewRequest {
  materialCode: string;
  addressText: string;
  lat: number;
  lng: number;
}

export async function createRequest(page: Page, req: NewRequest): Promise<string> {
  await page.goto('/requests/new');
  await page.locator('mat-select[formcontrolname="materialTypeCode"]').click();
  await page.locator(`mat-option[data-testid="material-option-${req.materialCode}"]`).click();
  await page.locator('input[formcontrolname="addressText"]').fill(req.addressText);
  await page.locator('input[formcontrolname="latitude"]').fill(String(req.lat));
  await page.locator('input[formcontrolname="longitude"]').fill(String(req.lng));
  await page.locator('button[type="submit"]').click();
  const uuidPattern = /\/requests\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$/i;
  await expect(page).toHaveURL(uuidPattern);
  return page.url().match(uuidPattern)![1];
}
