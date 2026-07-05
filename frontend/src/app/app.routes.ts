import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'requests',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/requests/request-list/request-list.component').then((m) => m.RequestListComponent)
      },
      {
        path: 'new',
        loadComponent: () =>
          import('./features/requests/new-request/new-request.component').then((m) => m.NewRequestComponent)
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./features/requests/request-detail/request-detail.component').then((m) => m.RequestDetailComponent)
      }
    ]
  }
];
