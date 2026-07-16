import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { provideTestTranslate } from '../../testing/translate-testing';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('starts unauthenticated with no stored session', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.role()).toBeNull();
  });

  it('persists the token and role on successful login', () => {
    service.login({ email: 'a@example.com', password: 'secret1234' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u1', role: 'HOUSEHOLD_SME' });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('HOUSEHOLD_SME');
    expect(service.accessToken()).toBe('token-abc');
    expect(sessionStorage.getItem('farragh_auth')).toContain('token-abc');
  });

  it('clears the session on logout', () => {
    service.login({ email: 'a@example.com', password: 'secret1234' }).subscribe();
    httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u1', role: 'HOUSEHOLD_SME' });

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('farragh_auth')).toBeNull();
  });
});
