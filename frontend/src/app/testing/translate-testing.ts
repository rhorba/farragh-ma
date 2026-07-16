import { Provider } from '@angular/core';
import { provideTranslateLoader, provideTranslateService } from '@ngx-translate/core';
import { StaticTranslateLoader } from '../i18n/static-translate-loader';

/**
 * Same static loader used in prod (see StaticTranslateLoader) - no HttpTestingController
 * flushing needed for translations, and specs asserting rendered French text keep working
 * unchanged since 'fr' is the default/fallback language in tests too.
 */
export function provideTestTranslate(): Provider[] {
  return provideTranslateService({
    lang: 'fr',
    fallbackLang: 'fr',
    loader: provideTranslateLoader(StaticTranslateLoader)
  });
}
