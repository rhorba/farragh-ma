import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AcceptedRequestsComponent } from './accepted-requests.component';
import { environment } from '../../../../environments/environment';
import { provideTestTranslate } from '../../../testing/translate-testing';

describe('AcceptedRequestsComponent', () => {
  let component: AcceptedRequestsComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<AcceptedRequestsComponent>>;
  let httpMock: HttpTestingController;
  const requestsUrl = `${environment.apiBaseUrl}/recyclers/requests`;

  function sample(status: string) {
    return {
      id: 'r1',
      materialTypeCode: 'PLASTIC',
      quantityDesc: '5kg',
      addressText: 'Test address',
      latitude: 34.02,
      longitude: -6.83,
      status,
      photoUrl: null,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z'
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AcceptedRequestsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    fixture = TestBed.createComponent(AcceptedRequestsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads accepted requests and shows a Planifier button for ACCEPTED', () => {
    httpMock.expectOne(requestsUrl).flush([sample('ACCEPTED')]);
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.requests()).toEqual([sample('ACCEPTED')]);
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('.request-card button');
    expect(button.textContent?.trim()).toBe('Planifier');
  });

  it('shows a Marquer terminée button for SCHEDULED and no button for COMPLETED', () => {
    httpMock.expectOne(requestsUrl).flush([sample('SCHEDULED'), { ...sample('COMPLETED'), id: 'r2' }]);
    fixture.detectChanges();

    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.request-card button'));
    expect(buttons).toHaveLength(1);
    expect(buttons[0].textContent?.trim()).toBe('Marquer terminée');
  });

  it('shows the empty state when there are no accepted requests', () => {
    httpMock.expectOne(requestsUrl).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.empty-state')).toBeTruthy();
  });

  it('shows the error state and retries on button click when the list fails to load', () => {
    httpMock.expectOne(requestsUrl).flush('server error', { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    expect(component.loadError()).toBe(true);
    const retryButton: HTMLButtonElement = fixture.nativeElement.querySelector('.error button');
    retryButton.click();
    httpMock.expectOne(requestsUrl).flush([]);
  });

  it('schedules an ACCEPTED request and updates it in place', () => {
    httpMock.expectOne(requestsUrl).flush([sample('ACCEPTED')]);
    fixture.detectChanges();

    component.schedule('r1');
    httpMock.expectOne(`${requestsUrl}/r1/schedule`).flush(sample('SCHEDULED'));

    expect(component.requests()).toEqual([sample('SCHEDULED')]);
    expect(component.updatingId()).toBeNull();
  });

  it('completes a SCHEDULED request and updates it in place', () => {
    httpMock.expectOne(requestsUrl).flush([sample('SCHEDULED')]);
    fixture.detectChanges();

    component.complete('r1');
    httpMock.expectOne(`${requestsUrl}/r1/complete`).flush(sample('COMPLETED'));

    expect(component.requests()).toEqual([sample('COMPLETED')]);
  });

  it('shows an error and reloads when a transition fails', () => {
    httpMock.expectOne(requestsUrl).flush([sample('ACCEPTED')]);
    fixture.detectChanges();

    component.schedule('r1');
    httpMock.expectOne(`${requestsUrl}/r1/schedule`).flush('conflict', { status: 409, statusText: 'Conflict' });
    httpMock.expectOne(requestsUrl).flush([sample('ACCEPTED')]);
    fixture.detectChanges();

    expect(component.updateError()).toBe('Impossible de mettre à jour cette demande. Réessayez.');
    expect(component.updatingId()).toBeNull();
  });
});
