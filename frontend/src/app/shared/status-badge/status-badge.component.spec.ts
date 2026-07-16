import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { StatusBadgeComponent } from './status-badge.component';
import { RequestStatus } from '../../features/requests/request.models';
import { provideTestTranslate } from '../../testing/translate-testing';

describe('StatusBadgeComponent', () => {
  function createWithStatus(status: RequestStatus) {
    const fixture = TestBed.createComponent(StatusBadgeComponent);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [StatusBadgeComponent], providers: [provideTestTranslate()] });
  });

  const cases: [RequestStatus, string][] = [
    ['POSTED', 'Publiée'],
    ['ACCEPTED', 'Acceptée'],
    ['SCHEDULED', 'Planifiée'],
    ['COMPLETED', 'Terminée'],
    ['CANCELLED', 'Annulée']
  ];

  it.each(cases)('renders the French label for %s', (status, expectedLabel) => {
    const fixture = createWithStatus(status);

    expect(fixture.componentInstance.label()).toBe(expectedLabel);
    const span: HTMLElement = fixture.nativeElement.querySelector('span.status-badge');
    expect(span.textContent?.trim()).toBe(expectedLabel);
    expect(span.classList.contains(status)).toBe(true);
    expect(span.getAttribute('aria-label')).toBe(expectedLabel);
  });

  it('re-renders with the Arabic label when the language switches', () => {
    const fixture = createWithStatus('ACCEPTED');
    const translate = TestBed.inject(TranslateService);

    translate.use('ar');
    fixture.detectChanges();

    const span: HTMLElement = fixture.nativeElement.querySelector('span.status-badge');
    expect(span.textContent?.trim()).toBe('مقبولة');
  });
});
