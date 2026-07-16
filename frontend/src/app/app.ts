import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Dir } from '@angular/cdk/bidi';
import { LanguageService } from './core/i18n/language.service';
import { LanguageSwitcherComponent } from './core/i18n/language-switcher.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Dir, LanguageSwitcherComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  readonly languageService = inject(LanguageService);
}
