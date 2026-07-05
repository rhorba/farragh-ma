import { Component, computed, input } from '@angular/core';
import { RequestStatus } from '../../features/requests/request.models';

const LABELS_FR: Record<RequestStatus, string> = {
  POSTED: 'Publiée',
  ACCEPTED: 'Acceptée',
  SCHEDULED: 'Planifiée',
  COMPLETED: 'Terminée',
  CANCELLED: 'Annulée'
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="status-badge" [class]="status()" role="status" [attr.aria-label]="label()">{{ label() }}</span>`,
  styleUrl: './status-badge.component.scss'
})
export class StatusBadgeComponent {
  readonly status = input.required<RequestStatus>();
  readonly label = computed(() => LABELS_FR[this.status()]);
}
