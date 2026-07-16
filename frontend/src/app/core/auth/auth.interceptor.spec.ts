import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { provideTestTranslate } from '../../testing/translate-testing';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslate()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('does not attach an Authorization header when logged out', () => {
    http.get('/api/v1/requests').subscribe();

    const req = httpMock.expectOne('/api/v1/requests');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('attaches the bearer token once logged in', () => {
    authService.login({ email: 'a@example.com', password: 'secret1234' }).subscribe();
    httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u1', role: 'HOUSEHOLD_SME' });

    http.get('/api/v1/requests').subscribe();

    const req = httpMock.expectOne('/api/v1/requests');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-abc');
    req.flush([]);
  });
});
