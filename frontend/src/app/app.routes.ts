import { Routes } from '@angular/router';
import { NotFoundComponent } from './features/static/not-found/not-found.component';

export const routes: Routes = [
  {
    path: 'posts',
    loadChildren: () =>
      import('./features/posts/posts.routes').then((m) => m.POSTS_ROUTES),
  },
  {
    path: 'login',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'profile',
    loadChildren: () =>
      import('./features/user/user.routes').then((m) => m.USER_ROUTES),
  },
  {
    path: 'admin',
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  { path: 'home', redirectTo: '', pathMatch: 'full' },
  // Named 404 page that screens can show for content that does not exist (a single word like /404
  // would be taken for an old category address and redirected to /category/404).
  { path: 'not-found', component: NotFoundComponent },
  {
    path: 'search',
    loadChildren: () =>
      import('./features/static/search/search.routes').then((m) => m.SEARCH_ROUTES),
  },
  {
    path: 'page',
    loadChildren: () =>
      import('./features/static/page-view/page-view.routes').then((m) => m.PAGE_VIEW_ROUTES),
  },
  {
    path: '',
    loadChildren: () =>
      import('./features/home/home.routes').then((m) => m.HOME_ROUTES),
  },
  { path: '**', component: NotFoundComponent },
];
