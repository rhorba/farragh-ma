import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RequestsService } from '../requests.service';
import { MATERIAL_TYPES } from '../request.models';

@Component({
  selector: 'app-new-request',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './new-request.component.html',
  styleUrl: './new-request.component.scss'
})
export class NewRequestComponent {
  private readonly fb = inject(FormBuilder);
  private readonly requestsService = inject(RequestsService);
  private readonly router = inject(Router);

  readonly materialTypes = MATERIAL_TYPES;
  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  readonly form = this.fb.nonNullable.group({
    materialTypeCode: ['PLASTIC', Validators.required],
    quantityDesc: [''],
    addressText: ['', Validators.required],
    latitude: [null as number | null, [Validators.required]],
    longitude: [null as number | null, [Validators.required]]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    const value = this.form.getRawValue();
    this.requestsService.create({
      materialTypeCode: value.materialTypeCode,
      quantityDesc: value.quantityDesc || undefined,
      addressText: value.addressText,
      latitude: value.latitude!,
      longitude: value.longitude!
    }).subscribe({
      next: (created) => this.router.navigate(['/requests', created.id]),
      error: () => {
        this.errorMessage.set("Erreur lors de la publication de la demande. Réessayez.");
        this.submitting.set(false);
      }
    });
  }
}
