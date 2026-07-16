import { TestBed } from '@angular/core/testing';
import { LanguageService } from './language.service';
import { provideTestTranslate } from '../../testing/translate-testing';

describe('LanguageService', () => {
  let service: LanguageService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTestTranslate()] });
    service = TestBed.inject(LanguageService);
  });

  it('defaults to French / ltr when nothing is stored', () => {
    expect(service.currentLang()).toBe('fr');
    expect(service.direction()).toBe('ltr');
  });

  it('switches to Arabic / rtl and persists the choice', () => {
    service.setLanguage('ar');
    TestBed.tick();

    expect(service.currentLang()).toBe('ar');
    expect(service.direction()).toBe('rtl');
    expect(localStorage.getItem('farragh_lang')).toBe('ar');
    expect(document.documentElement.lang).toBe('ar');
  });

  it('reads a previously stored language on construction', () => {
    localStorage.setItem('farragh_lang', 'ar');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideTestTranslate()] });

    const fresh = TestBed.inject(LanguageService);

    expect(fresh.currentLang()).toBe('ar');
  });

  it('seedFromServer applies a valid server preference', () => {
    service.seedFromServer('ar');
    expect(service.currentLang()).toBe('ar');
  });

  it('seedFromServer ignores an undefined/invalid preference', () => {
    service.seedFromServer(undefined);
    expect(service.currentLang()).toBe('fr');
  });
});
