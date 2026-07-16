import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { RequestResponseDto, RequestStatus } from '../../requests/request.models';
import { AdminService } from '../admin.service';
import { AdminUserDto } from '../admin.models';

const ROLES = ['HOUSEHOLD_SME', 'RECYCLER', 'MUNICIPALITY', 'ADMIN'] as const;
const STATUSES: RequestStatus[] = ['POSTED', 'ACCEPTED', 'SCHEDULED', 'COMPLETED', 'CANCELLED'];

@Component({
  selector: 'app-admin-search',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    StatusBadgeComponent,
    TranslatePipe
  ],
  templateUrl: './admin-search.component.html',
  styleUrl: './admin-search.component.scss'
})
export class AdminSearchComponent {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);

  readonly roles = ROLES;
  readonly statuses = STATUSES;

  readonly userFilterForm = this.fb.nonNullable.group({ email: '', role: '' });
  readonly requestFilterForm = this.fb.nonNullable.group({ status: '' });

  readonly users = signal<AdminUserDto[]>([]);
  readonly usersSearched = signal(false);
  readonly usersLoading = signal(false);

  readonly requests = signal<RequestResponseDto[]>([]);
  readonly requestsSearched = signal(false);
  readonly requestsLoading = signal(false);

  searchUsers(): void {
    const { email, role } = this.userFilterForm.getRawValue();
    this.usersLoading.set(true);
    this.adminService.searchUsers(email || null, role || null).subscribe({
      next: (page) => {
        this.users.set(page.content);
        this.usersSearched.set(true);
        this.usersLoading.set(false);
      },
      error: () => {
        this.usersLoading.set(false);
        this.usersSearched.set(true);
      }
    });
  }

  searchRequests(): void {
    const { status } = this.requestFilterForm.getRawValue();
    this.requestsLoading.set(true);
    this.adminService.searchRequests(status || null).subscribe({
      next: (page) => {
        this.requests.set(page.content);
        this.requestsSearched.set(true);
        this.requestsLoading.set(false);
      },
      error: () => {
        this.requestsLoading.set(false);
        this.requestsSearched.set(true);
      }
    });
  }
}
