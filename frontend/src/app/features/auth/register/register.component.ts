import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../../core/auth/auth.service';
import { Role } from '../../../core/auth/auth.models';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  readonly roles: { value: Role; label: string }[] = [
    { value: 'HOUSEHOLD_SME', label: 'Ménage / PME' },
    { value: 'RECYCLER', label: 'Recycleur certifié' },
    { value: 'MUNICIPALITY', label: 'Municipalité' }
  ];

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(10)]],
    fullName: ['', Validators.required],
    phone: [''],
    role: ['HOUSEHOLD_SME' as Role, Validators.required]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    this.authService.register(this.form.getRawValue()).subscribe({
      next: () => this.redirectAfterAuth(),
      error: () => {
        this.errorMessage.set("Impossible de créer le compte. Vérifiez vos informations.");
        this.submitting.set(false);
      }
    });
  }

  private redirectAfterAuth(): void {
    const role = this.authService.role();
    if (role === 'RECYCLER') {
      this.router.navigate(['/recycler']);
    } else {
      this.router.navigate([role === 'HOUSEHOLD_SME' ? '/requests' : '/']);
    }
  }
}
