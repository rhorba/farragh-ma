import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { MunicipalityService } from '../municipality.service';
import { SubscriptionResponseDto } from '../municipality.models';

@Component({
  selector: 'app-municipality-subscriptions',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, TranslatePipe],
  templateUrl: './municipality-subscriptions.component.html',
  styleUrl: './municipality-subscriptions.component.scss'
})
export class MunicipalitySubscriptionsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly municipalityService = inject(MunicipalityService);
  private readonly translate = inject(TranslateService);

  readonly subscriptions = signal<SubscriptionResponseDto[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly overlapWarning = signal(false);

  readonly form = this.fb.nonNullable.group({
    centerLatitude: [null as number | null, Validators.required],
    centerLongitude: [null as number | null, Validators.required],
    radiusM: [3000, [Validators.required, Validators.min(50)]]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.municipalityService.listMySubscriptions().subscribe({
      next: (subscriptions) => {
        this.subscriptions.set(subscriptions);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.subscribe(false);
  }

  confirmOverlap(): void {
    this.subscribe(true);
  }

  private subscribe(confirmOverlap: boolean): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.submitting.set(true);
    const value = this.form.getRawValue();

    this.municipalityService.subscribe({ ...value, polygon: null, confirmOverlap }).subscribe({
      next: (result) => {
        this.submitting.set(false);
        if (result.overlapWarning && !result.subscription) {
          this.overlapWarning.set(true);
          return;
        }
        this.overlapWarning.set(false);
        this.successMessage.set(this.translate.instant('municipality.subscriptions.success'));
        this.load();
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set(this.translate.instant('municipality.subscriptions.error'));
      }
    });
  }
}
