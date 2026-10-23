import { Routes } from '@angular/router';
import { roleGuard } from '../../guards/role.guard';
import { AdminUsersComponent } from './admin-users/admin-users.component';
import { AdminCategoriesComponent } from './admin-categories/admin-categories.component';
import { AdminPostsComponent } from './admin-posts/admin-posts.component';
import { AdminPagesComponent } from './admin-pages/admin-pages.component';
import { AdminSocialLinksComponent } from './admin-social-links/admin-social-links.component';
import { AdminBrandingComponent } from './admin-branding/admin-branding.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'users',
    component: AdminUsersComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'posts',
    component: AdminPostsComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'categories',
    component: AdminCategoriesComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'pages',
    component: AdminPagesComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'social-links',
    component: AdminSocialLinksComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'branding',
    component: AdminBrandingComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  { path: '', redirectTo: 'users', pathMatch: 'full' },
];
