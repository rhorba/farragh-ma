import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../core/i18n/language.service';
import { AdminService } from '../admin.service';
import {
  AnalyticsGranularity,
  RequestStatusKey,
  RequestsAnalyticsSummaryDto,
  RequestsTimeSeriesPointDto
} from '../admin.models';

const STATUS_ORDER: RequestStatusKey[] = ['POSTED', 'ACCEPTED', 'SCHEDULED', 'COMPLETED', 'CANCELLED'];

// Reuses the app's existing status-badge colors (status-badge.component.scss) for visual
// consistency between the badges seen elsewhere and this chart, rather than a second palette.
const STATUS_COLORS: Record<RequestStatusKey, string> = {
  POSTED: '#2563EB',
  ACCEPTED: '#16A34A',
  SCHEDULED: '#F59E0B',
  COMPLETED: '#1B7F4D',
  CANCELLED: '#DC2626'
};

const CHART_WIDTH = 640;
const CHART_HEIGHT = 220;
const CHART_PADDING = { top: 16, right: 16, bottom: 28, left: 36 };

interface StatusBarViewModel {
  status: RequestStatusKey;
  count: number;
  widthPct: number;
  color: string;
}

interface LinePoint {
  x: number;
  y: number;
}

@Component({
  selector: 'app-admin-analytics',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, RouterLink, TranslatePipe],
  templateUrl: './admin-analytics.component.html',
  styleUrl: './admin-analytics.component.scss'
})
export class AdminAnalyticsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly languageService = inject(LanguageService);

  readonly granularities: AnalyticsGranularity[] = ['DAY', 'WEEK', 'MONTH'];
  readonly statusOrder = STATUS_ORDER;

  readonly filterForm = this.fb.nonNullable.group({
    from: defaultFromDate(),
    to: defaultToDate(),
    granularity: 'DAY' as AnalyticsGranularity
  });

  readonly loading = signal(false);
  readonly summary = signal<RequestsAnalyticsSummaryDto | null>(null);
  readonly timeSeries = signal<RequestsTimeSeriesPointDto[]>([]);
  readonly showTable = signal(false);
  readonly hoveredIndex = signal<number | null>(null);

  readonly statusBars = computed<StatusBarViewModel[]>(() => {
    const s = this.summary();
    if (!s) return [];
    const max = Math.max(1, ...STATUS_ORDER.map((status) => s.countsByStatus[status] ?? 0));
    return STATUS_ORDER.map((status) => {
      const count = s.countsByStatus[status] ?? 0;
      return { status, count, widthPct: (count / max) * 100, color: STATUS_COLORS[status] };
    });
  });

  readonly chartMaxY = computed(() => {
    const points = this.timeSeries();
    return Math.max(1, ...points.map((p) => Math.max(p.created, p.completed)));
  });

  readonly createdPoints = computed<LinePoint[]>(() => this.toLinePoints((p) => p.created));
  readonly completedPoints = computed<LinePoint[]>(() => this.toLinePoints((p) => p.completed));
  readonly createdPath = computed(() => toPath(this.createdPoints()));
  readonly completedPath = computed(() => toPath(this.completedPoints()));

  readonly hoveredPoint = computed(() => {
    const index = this.hoveredIndex();
    const points = this.timeSeries();
    return index !== null ? points[index] : null;
  });

  readonly chartWidth = CHART_WIDTH;
  readonly chartHeight = CHART_HEIGHT;
  readonly plotLeft = CHART_PADDING.left;
  readonly plotRight = CHART_WIDTH - CHART_PADDING.right;
  readonly plotTop = CHART_PADDING.top;
  readonly plotBottom = CHART_HEIGHT - CHART_PADDING.bottom;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const { from, to, granularity } = this.filterForm.getRawValue();
    const fromInstant = toDayStartInstant(from);
    const toInstant = toDayEndInstant(to);
    this.loading.set(true);
    this.adminService.getRequestsSummary(fromInstant, toInstant).subscribe((summary) => this.summary.set(summary));
    this.adminService.getRequestsTimeSeries(fromInstant, toInstant, granularity).subscribe({
      next: (points) => {
        this.timeSeries.set(points);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  toggleTable(): void {
    this.showTable.update((v) => !v);
  }

  exportCsv(): void {
    const { from, to, granularity } = this.filterForm.getRawValue();
    this.adminService.exportRequestsTimeSeriesCsv(toDayStartInstant(from), toDayEndInstant(to), granularity).subscribe((csv) => {
      const blob = new Blob([csv], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'requests-analytics.csv';
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  onHover(clientX: number, svgElement: Element): void {
    const points = this.createdPoints();
    if (points.length === 0) return;
    const rect = svgElement.getBoundingClientRect();
    const scale = this.chartWidth / rect.width;
    const localX = (clientX - rect.left) * scale;
    let nearest = 0;
    let nearestDist = Infinity;
    points.forEach((p, i) => {
      const dist = Math.abs(p.x - localX);
      if (dist < nearestDist) {
        nearestDist = dist;
        nearest = i;
      }
    });
    this.hoveredIndex.set(nearest);
  }

  clearHover(): void {
    this.hoveredIndex.set(null);
  }

  drillIntoStatus(status: RequestStatusKey): void {
    const { from, to } = this.filterForm.getRawValue();
    this.router.navigate(['/admin/search'], { queryParams: { status, createdFrom: from, createdTo: to } });
  }

  drillIntoBucket(point: RequestsTimeSeriesPointDto): void {
    const bucketDate = point.bucket.slice(0, 10);
    this.router.navigate(['/admin/search'], { queryParams: { createdFrom: bucketDate, createdTo: bucketDate } });
  }

  statusLabel(status: RequestStatusKey): string {
    return this.translate.instant('status.' + status);
  }

  formatBucket(bucket: string): string {
    return new Date(bucket).toLocaleDateString(this.languageService.currentLang());
  }

  private toLinePoints(select: (p: RequestsTimeSeriesPointDto) => number): LinePoint[] {
    const points = this.timeSeries();
    const maxY = this.chartMaxY();
    const plotWidth = this.plotRight - this.plotLeft;
    const plotHeight = this.plotBottom - this.plotTop;
    if (points.length === 0) return [];
    if (points.length === 1) {
      const y = this.plotBottom - (select(points[0]) / maxY) * plotHeight;
      return [{ x: this.plotLeft + plotWidth / 2, y }];
    }
    return points.map((p, i) => ({
      x: this.plotLeft + (i / (points.length - 1)) * plotWidth,
      y: this.plotBottom - (select(p) / maxY) * plotHeight
    }));
  }
}

function toPath(points: LinePoint[]): string {
  if (points.length === 0) return '';
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
}

function toDayStartInstant(dateStr: string): string | null {
  return dateStr ? `${dateStr}T00:00:00Z` : null;
}

function toDayEndInstant(dateStr: string): string | null {
  return dateStr ? `${dateStr}T23:59:59Z` : null;
}

function defaultFromDate(): string {
  const d = new Date();
  d.setDate(d.getDate() - 90);
  return d.toISOString().slice(0, 10);
}

function defaultToDate(): string {
  return new Date().toISOString().slice(0, 10);
}
