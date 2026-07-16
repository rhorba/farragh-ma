import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { RequestResponseDto } from '../../requests/request.models';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { RecyclersService } from '../recyclers.service';

@Component({
  selector: 'app-recycler-feed',
  standalone: true,
  imports: [RouterLink, MatButtonModule, StatusBadgeComponent, TranslatePipe],
  templateUrl: './recycler-feed.component.html',
  styleUrl: './recycler-feed.component.scss'
})
export class RecyclerFeedComponent implements OnInit {
  private readonly recyclersService = inject(RecyclersService);
  private readonly translate = inject(TranslateService);

  readonly requests = signal<RequestResponseDto[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly acceptingId = signal<string | null>(null);
  readonly acceptError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.recyclersService.getFeed().subscribe({
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

  accept(id: string): void {
    this.acceptError.set(null);
    this.acceptingId.set(id);
    this.recyclersService.accept(id).subscribe({
      next: () => {
        this.requests.update((list) => list.filter((r) => r.id !== id));
        this.acceptingId.set(null);
      },
      error: () => {
        this.acceptingId.set(null);
        this.acceptError.set(this.translate.instant('recyclers.feed.alreadyTaken'));
        this.load();
      }
    });
  }
}
