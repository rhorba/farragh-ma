import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { RequestResponseDto, RequestStatus } from '../../requests/request.models';
import { AdminService } from '../admin.service';
import { AdminActionLogDto, AdminUserDto } from '../admin.models';

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
    TranslatePipe,
    DatePipe
  ],
  templateUrl: './admin-search.component.html',
  styleUrl: './admin-search.component.scss'
})
export class AdminSearchComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);

  readonly roles = ROLES;
  readonly statuses = STATUSES;
  readonly currentUserId = this.authService.userId;

  readonly userFilterForm = this.fb.nonNullable.group({ email: '', role: '' });
  readonly requestFilterForm = this.fb.nonNullable.group({ status: '' });

  readonly users = signal<AdminUserDto[]>([]);
  readonly usersSearched = signal(false);
  readonly usersLoading = signal(false);
  readonly togglingUserId = signal<string | null>(null);

  readonly requests = signal<RequestResponseDto[]>([]);
  readonly requestsSearched = signal(false);
  readonly requestsLoading = signal(false);

  readonly actionLog = signal<AdminActionLogDto[]>([]);
  readonly actionLogLoading = signal(false);

  ngOnInit(): void {
    this.loadActionLog();
  }

  toggleActive(user: AdminUserDto): void {
    this.togglingUserId.set(user.id);
    const action$ = user.active
      ? this.adminService.deactivateUser(user.id)
      : this.adminService.reactivateUser(user.id);
    action$.subscribe({
      next: (updated) => {
        this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.togglingUserId.set(null);
        this.loadActionLog();
      },
      error: () => this.togglingUserId.set(null)
    });
  }

  private loadActionLog(): void {
    this.actionLogLoading.set(true);
    this.adminService.getActionLog().subscribe({
      next: (page) => {
        this.actionLog.set(page.content);
        this.actionLogLoading.set(false);
      },
      error: () => this.actionLogLoading.set(false)
    });
  }

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
