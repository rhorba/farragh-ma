import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LoginComponent } from './login.component';
import { environment } from '../../../../environments/environment';
import { provideTestTranslate } from '../../../testing/translate-testing';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<LoginComponent>>;
  let httpMock: HttpTestingController;
  let router: Router;
  const loginUrl = `${environment.apiBaseUrl}/auth/login`;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('marks the form invalid and does not submit when fields are empty', () => {
    component.submit();

    httpMock.expectNone(loginUrl);
    expect(component.form.get('email')?.touched).toBe(true);
    expect(component.form.get('password')?.touched).toBe(true);
  });

  it('logs in and redirects household users to /requests', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.form.setValue({ email: 'a@example.com', password: 'secret1234' });

    component.submit();
    httpMock.expectOne(loginUrl)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u1', role: 'HOUSEHOLD_SME' });

    expect(navigateSpy).toHaveBeenCalledWith(['/requests']);
  });

  it('redirects recycler users to /recycler', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.form.setValue({ email: 'r@example.com', password: 'secret1234' });

    component.submit();
    httpMock.expectOne(loginUrl)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u2', role: 'RECYCLER' });

    expect(navigateSpy).toHaveBeenCalledWith(['/recycler']);
  });

  it('redirects admin users to /admin', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.form.setValue({ email: 'admin@example.com', password: 'secret1234' });

    component.submit();
    httpMock.expectOne(loginUrl)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u3', role: 'ADMIN' });

    expect(navigateSpy).toHaveBeenCalledWith(['/admin']);
  });

  it('redirects municipality users to /municipality', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.form.setValue({ email: 'muni@example.com', password: 'secret1234' });

    component.submit();
    httpMock.expectOne(loginUrl)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u4', role: 'MUNICIPALITY' });

    expect(navigateSpy).toHaveBeenCalledWith(['/municipality']);
  });

  it('sets an error message and resets submitting when login fails', () => {
    component.form.setValue({ email: 'a@example.com', password: 'wrongpass' });

    component.submit();
    httpMock.expectOne(loginUrl).flush('unauthorized', { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Email ou mot de passe incorrect.');
    expect(component.submitting()).toBe(false);
    expect(fixture.nativeElement.querySelector('.error').textContent).toContain('incorrect');
  });
});
