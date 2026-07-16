import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { adminGuard } from './admin.guard';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { provideTestTranslate } from '../../testing/translate-testing';

describe('adminGuard', () => {
  let authService: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  function runGuard(): boolean {
    return TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never)) as boolean;
  }

  it('blocks and redirects to /login when not authenticated', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    expect(runGuard()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('blocks a logged-in recycler account (wrong role)', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    authService.login({ email: 'r@example.com', password: 'secret1234' }).subscribe();
    httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u1', role: 'RECYCLER' });

    expect(runGuard()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('allows navigation for a logged-in admin', () => {
    authService.login({ email: 'a@example.com', password: 'secret1234' }).subscribe();
    httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u1', role: 'ADMIN' });

    expect(runGuard()).toBe(true);
  });
});
