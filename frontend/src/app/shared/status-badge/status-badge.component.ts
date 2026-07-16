import { Component, computed, inject, input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RequestStatus } from '../../features/requests/request.models';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="status-badge" [class]="status()" role="status" [attr.aria-label]="label()">{{ label() }}</span>`,
  styleUrl: './status-badge.component.scss'
})
export class StatusBadgeComponent {
  private readonly translate = inject(TranslateService);

  readonly status = input.required<RequestStatus>();
  readonly label = computed(() => {
    this.translate.currentLang();
    return this.translate.instant(`status.${this.status()}`);
  });
}
