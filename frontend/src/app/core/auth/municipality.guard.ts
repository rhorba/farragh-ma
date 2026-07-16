import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const municipalityGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated() && authService.role() === 'MUNICIPALITY') {
    return true;
  }
  router.navigate(['/login']);
  return false;
};
