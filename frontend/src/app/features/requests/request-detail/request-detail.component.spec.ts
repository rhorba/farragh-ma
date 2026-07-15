import { TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RequestDetailComponent } from './request-detail.component';
import { environment } from '../../../../environments/environment';

describe('RequestDetailComponent', () => {
  let httpMock: HttpTestingController;
  let fixture: ReturnType<typeof TestBed.createComponent<RequestDetailComponent>>;
  const detailUrl = `${environment.apiBaseUrl}/requests/req-1`;

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

  function createComponent() {
    TestBed.configureTestingModule({
      imports: [RequestDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'req-1' }) } }
        }
      ]
    });
    fixture = TestBed.createComponent(RequestDetailComponent);
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  afterEach(() => {
    httpMock.verify();
  });

  it('loads the request detail and renders it, including the optional quantityDesc row', () => {
    const component = createComponent();
    httpMock.expectOne(detailUrl).flush(sampleRequest);
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.notFound()).toBe(false);
    expect(component.request()).toEqual(sampleRequest);
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('PLASTIC');
    expect(fixture.nativeElement.textContent).toContain('5kg');
  });

  it('shows the not-found state when the request fails to load', () => {
    const component = createComponent();
    httpMock.expectOne(detailUrl).flush('not found', { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.notFound()).toBe(true);
    expect(fixture.nativeElement.querySelector('.error').textContent).toContain('introuvable');
  });

  it('shows the cancel button for an active request', () => {
    const component = createComponent();
    httpMock.expectOne(detailUrl).flush(sampleRequest);
    fixture.detectChanges();

    expect(component.canCancel()).toBe(true);
    expect(fixture.nativeElement.querySelector('button')).toBeTruthy();
  });

  it('hides the cancel button for a completed request', () => {
    const component = createComponent();
    httpMock.expectOne(detailUrl).flush({ ...sampleRequest, status: 'COMPLETED' });
    fixture.detectChanges();

    expect(component.canCancel()).toBe(false);
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('cancels the request and updates state and DOM on success', () => {
    const component = createComponent();
    httpMock.expectOne(detailUrl).flush(sampleRequest);
    fixture.detectChanges();

    component.cancel();
    expect(component.cancelling()).toBe(true);
    httpMock.expectOne(`${detailUrl}/cancel`).flush({ ...sampleRequest, status: 'CANCELLED' });
    fixture.detectChanges();

    expect(component.request()?.status).toBe('CANCELLED');
    expect(component.cancelling()).toBe(false);
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('resets cancelling without changing the request when cancel fails', () => {
    const component = createComponent();
    httpMock.expectOne(detailUrl).flush(sampleRequest);
    fixture.detectChanges();

    component.cancel();
    httpMock.expectOne(`${detailUrl}/cancel`).flush('conflict', { status: 409, statusText: 'Conflict' });

    expect(component.cancelling()).toBe(false);
    expect(component.request()).toEqual(sampleRequest);
  });
});
