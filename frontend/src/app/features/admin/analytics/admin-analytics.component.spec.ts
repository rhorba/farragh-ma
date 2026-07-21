import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminAnalyticsComponent } from './admin-analytics.component';
import { environment } from '../../../../environments/environment';
import { provideTestTranslate } from '../../../testing/translate-testing';

describe('AdminAnalyticsComponent', () => {
  let component: AdminAnalyticsComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<AdminAnalyticsComponent>>;
  let httpMock: HttpTestingController;
  let router: Router;
  const summaryUrl = `${environment.apiBaseUrl}/admin/analytics/requests/summary`;
  const timeseriesUrl = `${environment.apiBaseUrl}/admin/analytics/requests/timeseries`;
  const exportUrl = `${environment.apiBaseUrl}/admin/analytics/requests/export`;

  const sampleSummary = {
    from: '2026-01-01T00:00:00Z',
    to: '2026-01-31T00:00:00Z',
    total: 3,
    countsByStatus: { POSTED: 1, ACCEPTED: 0, SCHEDULED: 0, COMPLETED: 2, CANCELLED: 0 }
  };
  const samplePoints = [
    { bucket: '2026-01-05T00:00:00Z', created: 2, completed: 0 },
    { bucket: '2026-01-06T00:00:00Z', created: 1, completed: 2 }
  ];

  function init() {
    TestBed.configureTestingModule({
      imports: [AdminAnalyticsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideTestTranslate()]
    });
    fixture = TestBed.createComponent(AdminAnalyticsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpMock.verify();
  });

  it('loads the summary and time series on init and renders the stat tile and status bars', () => {
    init();
    httpMock.expectOne((r) => r.url === summaryUrl).flush(sampleSummary);
    httpMock.expectOne((r) => r.url === timeseriesUrl).flush(samplePoints);
    fixture.detectChanges();

    expect(component.summary()?.total).toBe(3);
    expect(component.timeSeries()).toHaveLength(2);
    expect(fixture.nativeElement.querySelector('.stat-value').textContent).toContain('3');
    const bars = fixture.nativeElement.querySelectorAll('.status-bar-row');
    expect(bars).toHaveLength(5);
  });

  it('sends the default 90-day range and DAY granularity on the initial load', () => {
    init();
    const summaryReq = httpMock.expectOne((r) => r.url === summaryUrl);
    expect(summaryReq.request.params.get('from')).toMatch(/T00:00:00Z$/);
    expect(summaryReq.request.params.get('to')).toMatch(/T23:59:59Z$/);
    summaryReq.flush(sampleSummary);
    const seriesReq = httpMock.expectOne((r) => r.url === timeseriesUrl);
    expect(seriesReq.request.params.get('granularity')).toBe('DAY');
    seriesReq.flush(samplePoints);
  });

  it('drills into a status bar by navigating to admin search with the status and range prefilled', () => {
    init();
    httpMock.expectOne((r) => r.url === summaryUrl).flush(sampleSummary);
    httpMock.expectOne((r) => r.url === timeseriesUrl).flush(samplePoints);
    fixture.detectChanges();
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.drillIntoStatus('COMPLETED');

    expect(navigateSpy).toHaveBeenCalledWith(['/admin/search'], {
      queryParams: {
        status: 'COMPLETED',
        createdFrom: component.filterForm.getRawValue().from,
        createdTo: component.filterForm.getRawValue().to
      }
    });
  });

  it('drills into a time-series bucket by navigating with that day as both bounds', () => {
    init();
    httpMock.expectOne((r) => r.url === summaryUrl).flush(sampleSummary);
    httpMock.expectOne((r) => r.url === timeseriesUrl).flush(samplePoints);
    fixture.detectChanges();
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.drillIntoBucket(samplePoints[0]);

    expect(navigateSpy).toHaveBeenCalledWith(['/admin/search'], {
      queryParams: { createdFrom: '2026-01-05', createdTo: '2026-01-05' }
    });
  });

  it('toggles between the chart and the accessible table view', () => {
    init();
    httpMock.expectOne((r) => r.url === summaryUrl).flush(sampleSummary);
    httpMock.expectOne((r) => r.url === timeseriesUrl).flush(samplePoints);
    fixture.detectChanges();

    expect(component.showTable()).toBe(false);
    component.toggleTable();
    fixture.detectChanges();

    expect(component.showTable()).toBe(true);
    const rows = fixture.nativeElement.querySelectorAll('.results tbody tr');
    expect(rows).toHaveLength(2);
  });

  it('shows the empty state when there is no data in range', () => {
    init();
    httpMock.expectOne((r) => r.url === summaryUrl).flush(sampleSummary);
    httpMock.expectOne((r) => r.url === timeseriesUrl).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.empty-state')).toBeTruthy();
  });

  it('requests a CSV export with the current filters', () => {
    init();
    httpMock.expectOne((r) => r.url === summaryUrl).flush(sampleSummary);
    httpMock.expectOne((r) => r.url === timeseriesUrl).flush(samplePoints);
    fixture.detectChanges();

    component.exportCsv();
    const req = httpMock.expectOne((r) => r.url === exportUrl);
    expect(req.request.params.get('granularity')).toBe('DAY');
    req.flush('bucket,created,completed\n');
  });

  it('does not blow up when the time series request fails', () => {
    init();
    httpMock.expectOne((r) => r.url === summaryUrl).flush(sampleSummary);
    httpMock.expectOne((r) => r.url === timeseriesUrl).flush('error', { status: 500, statusText: 'Internal Server Error' });

    expect(component.loading()).toBe(false);
  });
});
