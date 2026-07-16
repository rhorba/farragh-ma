import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { TranslateLoader, TranslationObject } from '@ngx-translate/core';
import { translations } from './translations';

/**
 * Translations are bundled TS objects (not fetched JSON) - this is a small app with a fixed,
 * developer-maintained set of strings, so a build-time bundle avoids an extra HTTP round trip
 * and keeps prod/test behavior identical (no HttpTestingController flushing needed in specs).
 */
@Injectable()
export class StaticTranslateLoader extends TranslateLoader {
  getTranslation(lang: string): Observable<TranslationObject> {
    const dict = (translations as Record<string, TranslationObject>)[lang];
    return of(dict ?? translations.fr);
  }
}
