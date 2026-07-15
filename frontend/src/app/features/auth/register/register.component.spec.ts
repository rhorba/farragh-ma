import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RegisterComponent } from './register.component';
import { environment } from '../../../../environments/environment';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<RegisterComponent>>;
  let httpMock: HttpTestingController;
  let router: Router;
  const registerUrl = `${environment.apiBaseUrl}/auth/register`;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('defaults the role to HOUSEHOLD_SME', () => {
    expect(component.form.get('role')?.value).toBe('HOUSEHOLD_SME');
  });

  it('marks the form invalid and does not submit when required fields are missing', () => {
    component.submit();

    httpMock.expectNone(registerUrl);
    expect(component.form.get('email')?.touched).toBe(true);
    expect(component.form.get('fullName')?.touched).toBe(true);
  });

  it('rejects a password shorter than 10 characters and shows the mat-error', () => {
    component.form.patchValue({
      email: 'a@example.com',
      password: 'short',
      fullName: 'Test User'
    });
    component.form.get('password')?.markAsTouched();
    component.form.get('email')?.markAsTouched();
    fixture.detectChanges();

    expect(component.form.get('password')?.hasError('minlength')).toBe(true);
    expect(fixture.nativeElement.querySelector('mat-error').textContent).toContain('10 caractères');
  });

  it('registers a municipality user and redirects to /', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.form.setValue({
      email: 'muni@example.com',
      password: 'secret1234',
      fullName: 'City Hall',
      phone: '',
      role: 'MUNICIPALITY'
    });

    component.submit();
    httpMock.expectOne(registerUrl)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u3', role: 'MUNICIPALITY' });

    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });

  it('registers a recycler user and redirects to /recycler', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.form.setValue({
      email: 'recycler@example.com',
      password: 'secret1234',
      fullName: 'Recycle Co',
      phone: '0600000000',
      role: 'RECYCLER'
    });

    component.submit();
    httpMock.expectOne(registerUrl)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u4', role: 'RECYCLER' });

    expect(navigateSpy).toHaveBeenCalledWith(['/recycler']);
  });

  it('sets an error message and resets submitting when registration fails (e.g. duplicate email)', () => {
    component.form.setValue({
      email: 'dup@example.com',
      password: 'secret1234',
      fullName: 'Dup User',
      phone: '',
      role: 'HOUSEHOLD_SME'
    });

    component.submit();
    httpMock.expectOne(registerUrl).flush('conflict', { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Impossible de créer le compte. Vérifiez vos informations.');
    expect(component.submitting()).toBe(false);
    expect(fixture.nativeElement.querySelector('.error').textContent).toContain('Impossible');
  });
});
