import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { RequestResponseDto } from '../../requests/request.models';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { RecyclersService } from '../recyclers.service';

@Component({
  selector: 'app-accepted-requests',
  standalone: true,
  imports: [RouterLink, MatButtonModule, StatusBadgeComponent, TranslatePipe],
  templateUrl: './accepted-requests.component.html',
  styleUrl: './accepted-requests.component.scss'
})
export class AcceptedRequestsComponent implements OnInit {
  private readonly recyclersService = inject(RecyclersService);
  private readonly translate = inject(TranslateService);

  readonly requests = signal<RequestResponseDto[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly updatingId = signal<string | null>(null);
  readonly updateError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.recyclersService.listAccepted().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      }
    });
  }

  schedule(id: string): void {
    this.runTransition(id, this.recyclersService.schedule(id));
  }

  complete(id: string): void {
    this.runTransition(id, this.recyclersService.complete(id));
  }

  private runTransition(id: string, action: ReturnType<RecyclersService['schedule']>): void {
    this.updateError.set(null);
    this.updatingId.set(id);
    action.subscribe({
      next: (updated) => {
        this.requests.update((list) => list.map((r) => (r.id === id ? updated : r)));
        this.updatingId.set(null);
      },
      error: () => {
        this.updatingId.set(null);
        this.updateError.set(this.translate.instant('recyclers.accepted.updateError'));
        this.load();
      }
    });
  }
}
