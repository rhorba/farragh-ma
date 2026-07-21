import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, Role } from './auth.models';
import { LanguageService } from '../i18n/language.service';

const STORAGE_KEY = 'farragh_auth';

interface StoredAuth {
  accessToken: string;
  role: Role;
  userId: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authState = signal<StoredAuth | null>(this.readFromStorage());

  readonly isAuthenticated = computed(() => this.authState() !== null);
  readonly role = computed(() => this.authState()?.role ?? null);
  readonly accessToken = computed(() => this.authState()?.accessToken ?? null);
  readonly userId = computed(() => this.authState()?.userId ?? null);

  private readonly http = inject(HttpClient);
  private readonly languageService = inject(LanguageService);

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/register`, request)
      .pipe(tap((response) => this.persist(response)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, request)
      .pipe(tap((response) => this.persist(response)));
  }

  logout(): void {
    this.authState.set(null);
    sessionStorage.removeItem(STORAGE_KEY);
  }

  private persist(response: AuthResponse): void {
    const stored: StoredAuth = { accessToken: response.accessToken, role: response.role, userId: response.userId };
    this.authState.set(stored);
    // sessionStorage (not localStorage): cleared when the tab closes, limiting the token's XSS-exposure window (Security Baseline doc §5).
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
    this.languageService.seedFromServer(response.preferredLang);
  }

  private readFromStorage(): StoredAuth | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredAuth) : null;
  }
}
