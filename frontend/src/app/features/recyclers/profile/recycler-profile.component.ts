import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin } from 'rxjs';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { MATERIAL_TYPES } from '../../requests/request.models';
import { RecyclersService } from '../recyclers.service';

@Component({
  selector: 'app-recycler-profile',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, TranslatePipe],
  templateUrl: './recycler-profile.component.html',
  styleUrl: './recycler-profile.component.scss'
})
export class RecyclerProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly recyclersService = inject(RecyclersService);
  private readonly translate = inject(TranslateService);

  readonly materialTypes = MATERIAL_TYPES;
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  readonly form = this.fb.nonNullable.group({
    centerLatitude: [null as number | null, Validators.required],
    centerLongitude: [null as number | null, Validators.required],
    radiusM: [5000, [Validators.required, Validators.min(50)]],
    materialTypeCodes: [[] as string[], Validators.required]
  });

  ngOnInit(): void {
    this.recyclersService.getZone().subscribe({
      next: (zone) =>
        this.form.patchValue({
          centerLatitude: zone.centerLatitude ?? null,
          centerLongitude: zone.centerLongitude ?? null,
          radiusM: zone.radiusM ?? 5000
        }),
      error: () => {
        // No zone declared yet - keep the blank defaults.
      }
    });
    this.recyclersService.getMaterials().subscribe({
      next: (materials) => this.form.patchValue({ materialTypeCodes: materials.materialTypeCodes }),
      error: () => {
        // No materials declared yet.
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.submitting.set(true);
    const value = this.form.getRawValue();

    forkJoin([
      this.recyclersService.declareZone({
        centerLatitude: value.centerLatitude,
        centerLongitude: value.centerLongitude,
        radiusM: value.radiusM,
        polygon: null
      }),
      this.recyclersService.declareMaterials({ materialTypeCodes: value.materialTypeCodes })
    ]).subscribe({
      next: () => {
        this.successMessage.set(this.translate.instant('recyclers.profile.success'));
        this.submitting.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('recyclers.profile.error'));
        this.submitting.set(false);
      }
    });
  }
}
