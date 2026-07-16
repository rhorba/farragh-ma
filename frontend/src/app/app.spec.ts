import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { provideTestTranslate } from './testing/translate-testing';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideTestTranslate()]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('defaults to ltr direction for French', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const shell: HTMLElement = fixture.nativeElement.querySelector('.app-shell');
    expect(shell.getAttribute('dir')).toBe('ltr');
  });

  it('switches to rtl direction when Arabic is selected', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    fixture.componentInstance.languageService.setLanguage('ar');
    fixture.detectChanges();
    const shell: HTMLElement = fixture.nativeElement.querySelector('.app-shell');
    expect(shell.getAttribute('dir')).toBe('rtl');
  });
});
