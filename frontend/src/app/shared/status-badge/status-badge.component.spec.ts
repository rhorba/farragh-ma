import { TestBed } from '@angular/core/testing';
import { StatusBadgeComponent } from './status-badge.component';
import { RequestStatus } from '../../features/requests/request.models';

describe('StatusBadgeComponent', () => {
  function createWithStatus(status: RequestStatus) {
    const fixture = TestBed.createComponent(StatusBadgeComponent);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [StatusBadgeComponent] });
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
});
