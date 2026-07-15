import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NewRequestComponent } from './new-request.component';
import { environment } from '../../../../environments/environment';

describe('NewRequestComponent', () => {
  let component: NewRequestComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<NewRequestComponent>>;
  let httpMock: HttpTestingController;
  let router: Router;
  const requestsUrl = `${environment.apiBaseUrl}/requests`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NewRequestComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    fixture = TestBed.createComponent(NewRequestComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('marks the form invalid, does not submit, and shows mat-errors when required fields are missing', () => {
    component.submit();
    fixture.detectChanges();

    httpMock.expectNone(requestsUrl);
    expect(component.form.get('addressText')?.touched).toBe(true);
    expect(component.form.get('latitude')?.touched).toBe(true);
    const errors = fixture.nativeElement.querySelectorAll('mat-error');
    expect(errors.length).toBeGreaterThan(0);
  });

  it('creates a request and navigates to its detail page on success', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.form.setValue({
      materialTypeCode: 'PLASTIC',
      quantityDesc: '5kg',
      addressText: 'Test address',
      latitude: 34.02,
      longitude: -6.83
    });

    component.submit();
    const req = httpMock.expectOne(requestsUrl);
    expect(req.request.body.quantityDesc).toBe('5kg');
    req.flush({
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
    });

    expect(navigateSpy).toHaveBeenCalledWith(['/requests', 'req-1']);
  });

  it('omits an empty quantityDesc from the payload', () => {
    component.form.setValue({
      materialTypeCode: 'PLASTIC',
      quantityDesc: '',
      addressText: 'Test address',
      latitude: 34.02,
      longitude: -6.83
    });

    component.submit();
    const req = httpMock.expectOne(requestsUrl);
    expect(req.request.body.quantityDesc).toBeUndefined();
  });

  it('sets an error message and resets submitting when creation fails', () => {
    component.form.setValue({
      materialTypeCode: 'PLASTIC',
      quantityDesc: '',
      addressText: 'Test address',
      latitude: 34.02,
      longitude: -6.83
    });

    component.submit();
    httpMock.expectOne(requestsUrl).flush('server error', { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Erreur lors de la publication de la demande. Réessayez.');
    expect(component.submitting()).toBe(false);
    expect(fixture.nativeElement.querySelector('.error').textContent).toContain('Erreur');
  });
});
