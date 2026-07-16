import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { recyclerGuard } from './core/auth/recycler.guard';
import { adminGuard } from './core/auth/admin.guard';
import { municipalityGuard } from './core/auth/municipality.guard';

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
  },
  {
    path: 'recycler',
    canActivate: [recyclerGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'feed' },
      {
        path: 'feed',
        loadComponent: () =>
          import('./features/recyclers/feed/recycler-feed.component').then((m) => m.RecyclerFeedComponent)
      },
      {
        path: 'accepted',
        loadComponent: () =>
          import('./features/recyclers/accepted/accepted-requests.component').then((m) => m.AcceptedRequestsComponent)
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/recyclers/profile/recycler-profile.component').then((m) => m.RecyclerProfileComponent)
      }
    ]
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/search/admin-search.component').then((m) => m.AdminSearchComponent)
  },
  {
    path: 'municipality',
    canActivate: [municipalityGuard],
    loadComponent: () =>
      import('./features/municipality/subscriptions/municipality-subscriptions.component').then(
        (m) => m.MunicipalitySubscriptionsComponent
      )
  }
];
