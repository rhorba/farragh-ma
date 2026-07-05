import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { RequestsService } from '../requests.service';
import { RequestResponseDto } from '../request.models';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [RouterLink, MatButtonModule, StatusBadgeComponent, DatePipe],
  templateUrl: './request-detail.component.html',
  styleUrl: './request-detail.component.scss'
})
export class RequestDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly requestsService = inject(RequestsService);

  readonly request = signal<RequestResponseDto | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly cancelling = signal(false);

  private readonly id = this.route.snapshot.paramMap.get('id')!;

  readonly canCancel = () => {
    const r = this.request();
    return r !== null && r.status !== 'COMPLETED' && r.status !== 'CANCELLED';
  };

  ngOnInit(): void {
    this.requestsService.getMine(this.id).subscribe({
      next: (r) => {
        this.request.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      }
    });
  }

  cancel(): void {
    this.cancelling.set(true);
    this.requestsService.cancel(this.id).subscribe({
      next: (r) => {
        this.request.set(r);
        this.cancelling.set(false);
      },
      error: () => {
        this.cancelling.set(false);
      }
    });
  }
}
