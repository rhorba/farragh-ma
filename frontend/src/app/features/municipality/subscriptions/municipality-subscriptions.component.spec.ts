import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MunicipalitySubscriptionsComponent } from './municipality-subscriptions.component';
import { environment } from '../../../../environments/environment';
import { provideTestTranslate } from '../../../testing/translate-testing';

describe('MunicipalitySubscriptionsComponent', () => {
  let component: MunicipalitySubscriptionsComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<MunicipalitySubscriptionsComponent>>;
  let httpMock: HttpTestingController;
  const subscriptionsUrl = `${environment.apiBaseUrl}/municipality/subscriptions`;

  function sample(overrides: Partial<{ id: string; active: boolean }> = {}) {
    return {
      id: overrides.id ?? 's1',
      centerLatitude: 33.5731,
      centerLongitude: -7.5898,
      radiusM: 3000,
      polygon: null,
      active: overrides.active ?? true,
      createdAt: '2026-01-01T00:00:00Z'
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MunicipalitySubscriptionsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    fixture = TestBed.createComponent(MunicipalitySubscriptionsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads existing subscriptions on init', () => {
    httpMock.expectOne(subscriptionsUrl).flush([sample()]);
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.subscriptions()).toEqual([sample()]);
  });

  it('shows the empty state when there are no subscriptions', () => {
    httpMock.expectOne(subscriptionsUrl).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.empty-state')).toBeTruthy();
  });

  it('shows the error state and retries on button click when the list fails to load', () => {
    httpMock.expectOne(subscriptionsUrl).flush('server error', { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    expect(component.loadError()).toBe(true);
    const retryButton: HTMLButtonElement = fixture.nativeElement.querySelector('.error button');
    retryButton.click();
    httpMock.expectOne(subscriptionsUrl).flush([]);
  });

  it('does not submit an invalid form', () => {
    httpMock.expectOne(subscriptionsUrl).flush([]);
    fixture.detectChanges();

    component.submit();
    httpMock.expectNone(subscriptionsUrl);
    expect(component.form.controls.centerLatitude.touched).toBe(true);
  });

  it('subscribes a non-overlapping zone and reloads the list', () => {
    httpMock.expectOne(subscriptionsUrl).flush([]);
    fixture.detectChanges();

    component.form.setValue({ centerLatitude: 33.5731, centerLongitude: -7.5898, radiusM: 3000 });
    component.submit();

    const req = httpMock.expectOne(subscriptionsUrl);
    expect(req.request.body.confirmOverlap).toBe(false);
    req.flush({ overlapWarning: false, subscription: sample() });

    httpMock.expectOne(subscriptionsUrl).flush([sample()]);

    expect(component.overlapWarning()).toBe(false);
    expect(component.successMessage()).toBe('Zone souscrite avec succès.');
  });

  it('shows an overlap warning without reloading the list, then confirms on demand', () => {
    httpMock.expectOne(subscriptionsUrl).flush([sample()]);
    fixture.detectChanges();

    component.form.setValue({ centerLatitude: 33.5731, centerLongitude: -7.5898, radiusM: 3000 });
    component.submit();

    httpMock.expectOne(subscriptionsUrl).flush({ overlapWarning: true, subscription: null });
    expect(component.overlapWarning()).toBe(true);

    component.confirmOverlap();
    const confirmReq = httpMock.expectOne(subscriptionsUrl);
    expect(confirmReq.request.body.confirmOverlap).toBe(true);
    confirmReq.flush({ overlapWarning: true, subscription: sample({ id: 's2' }) });

    httpMock.expectOne(subscriptionsUrl).flush([sample(), sample({ id: 's2' })]);
    expect(component.overlapWarning()).toBe(false);
  });

  it('shows an error message when subscribing fails', () => {
    httpMock.expectOne(subscriptionsUrl).flush([]);
    fixture.detectChanges();

    component.form.setValue({ centerLatitude: 33.5731, centerLongitude: -7.5898, radiusM: 3000 });
    component.submit();

    httpMock.expectOne(subscriptionsUrl).flush('server error', { status: 500, statusText: 'Internal Server Error' });

    expect(component.errorMessage()).toBe('Impossible de souscrire cette zone. Réessayez.');
  });
});
