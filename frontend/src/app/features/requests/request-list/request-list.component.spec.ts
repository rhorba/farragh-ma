import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RequestListComponent } from './request-list.component';
import { environment } from '../../../../environments/environment';

describe('RequestListComponent', () => {
  let component: RequestListComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<RequestListComponent>>;
  let httpMock: HttpTestingController;
  const requestsUrl = `${environment.apiBaseUrl}/requests`;

  const sampleRequest = {
    id: 'req-1',
    materialTypeCode: 'PLASTIC',
    quantityDesc: '5kg',
    addressText: 'Test address',
    latitude: 34.02,
    longitude: -6.83,
    status: 'POSTED',
    photoUrl: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RequestListComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    fixture = TestBed.createComponent(RequestListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads the list and renders a card per request', () => {
    httpMock.expectOne(requestsUrl).flush([sampleRequest]);
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.loadError()).toBe(false);
    expect(component.requests()).toEqual([sampleRequest]);
    const cards = fixture.nativeElement.querySelectorAll('li');
    expect(cards.length).toBe(1);
    expect(cards[0].textContent).toContain('Test address');
  });

  it('shows the empty state for an empty list', () => {
    httpMock.expectOne(requestsUrl).flush([]);
    fixture.detectChanges();

    expect(component.requests()).toEqual([]);
    expect(component.loadError()).toBe(false);
    expect(fixture.nativeElement.querySelector('.empty-state')).toBeTruthy();
  });

  it('shows the error state and retries on button click when the list fails to load', () => {
    httpMock.expectOne(requestsUrl).flush('server error', { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.loadError()).toBe(true);
    const retryButton: HTMLButtonElement = fixture.nativeElement.querySelector('.error button');
    expect(retryButton).toBeTruthy();

    retryButton.click();
    httpMock.expectOne(requestsUrl).flush([sampleRequest]);
    fixture.detectChanges();

    expect(component.loadError()).toBe(false);
    expect(component.requests()).toEqual([sampleRequest]);
  });

  it('re-fetches on load()', () => {
    httpMock.expectOne(requestsUrl).flush([sampleRequest]);

    component.load();
    expect(component.loading()).toBe(true);
    httpMock.expectOne(requestsUrl).flush([sampleRequest, sampleRequest]);

    expect(component.requests().length).toBe(2);
  });
});
