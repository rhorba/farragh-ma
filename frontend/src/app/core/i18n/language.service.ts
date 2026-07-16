import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type AppLang = 'fr' | 'ar';

const STORAGE_KEY = 'farragh_lang';
const SUPPORTED_LANGS: AppLang[] = ['fr', 'ar'];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);

  readonly currentLang = signal<AppLang>(this.readInitialLang());
  readonly direction = computed<'ltr' | 'rtl'>(() => (this.currentLang() === 'ar' ? 'rtl' : 'ltr'));

  constructor() {
    this.translate.addLangs(SUPPORTED_LANGS);
    this.translate.use(this.currentLang());
    effect(() => {
      document.documentElement.lang = this.currentLang();
    });
  }

  /** Called after login/register - the server's stored preference wins over any local override. */
  seedFromServer(preferredLang: string | undefined | null): void {
    if (preferredLang === 'ar' || preferredLang === 'fr') {
      this.setLanguage(preferredLang);
    }
  }

  setLanguage(lang: AppLang): void {
    if (this.currentLang() === lang) {
      return;
    }
    this.currentLang.set(lang);
    localStorage.setItem(STORAGE_KEY, lang);
    this.translate.use(lang);
  }

  private readInitialLang(): AppLang {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === 'ar' ? 'ar' : 'fr';
  }
}
