import { Component, inject } from '@angular/core';
import { LanguageService } from './language.service';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  templateUrl: './language-switcher.component.html',
  styleUrl: './language-switcher.component.scss'
})
export class LanguageSwitcherComponent {
  readonly languageService = inject(LanguageService);

  select(lang: 'fr' | 'ar'): void {
    this.languageService.setLanguage(lang);
  }
}
