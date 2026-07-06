import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { recyclerGuard } from './recycler.guard';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('recyclerGuard', () => {
  let authService: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  function runGuard(): boolean {
    return TestBed.runInInjectionContext(() => recyclerGuard({} as never, {} as never)) as boolean;
  }

  it('blocks and redirects to /login when not authenticated', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    expect(runGuard()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('blocks a logged-in household account (wrong role)', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    authService.login({ email: 'a@example.com', password: 'secret1234' }).subscribe();
    httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u1', role: 'HOUSEHOLD_SME' });

    expect(runGuard()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('allows navigation for a logged-in recycler', () => {
    authService.login({ email: 'r@example.com', password: 'secret1234' }).subscribe();
    httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ accessToken: 'token-abc', refreshToken: 'refresh-abc', userId: 'u1', role: 'RECYCLER' });

    expect(runGuard()).toBe(true);
  });
});
