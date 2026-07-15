import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => this.redirectAfterAuth(),
      error: () => {
        this.errorMessage.set('Email ou mot de passe incorrect.');
        this.submitting.set(false);
      }
    });
  }

  private redirectAfterAuth(): void {
    const role = this.authService.role();
    if (role === 'RECYCLER') {
      this.router.navigate(['/recycler']);
    } else if (role === 'ADMIN') {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate([role === 'HOUSEHOLD_SME' ? '/requests' : '/']);
    }
  }
}
