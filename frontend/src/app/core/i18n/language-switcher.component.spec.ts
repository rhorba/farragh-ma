import { TestBed } from '@angular/core/testing';
import { LanguageSwitcherComponent } from './language-switcher.component';
import { provideTestTranslate } from '../../testing/translate-testing';

describe('LanguageSwitcherComponent', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<LanguageSwitcherComponent>>;
  let component: LanguageSwitcherComponent;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [LanguageSwitcherComponent],
      providers: [provideTestTranslate()]
    });
    fixture = TestBed.createComponent(LanguageSwitcherComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('marks French as active by default', () => {
    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('button'));
    expect(buttons[0].classList).toContain('active');
    expect(buttons[1].classList).not.toContain('active');
  });

  it('switches to Arabic when the AR button is clicked', () => {
    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('button'));
    buttons[1].click();
    fixture.detectChanges();

    expect(component.languageService.currentLang()).toBe('ar');
    expect(buttons[1].classList).toContain('active');
    expect(buttons[0].classList).not.toContain('active');
  });
});
