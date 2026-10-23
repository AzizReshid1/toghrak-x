import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoleGuard } from '../../guards/role.guard';
import { AdminUsersComponent } from './admin-users/admin-users.component';
import { AdminCategoriesComponent } from './admin-categories/admin-categories.component';
import { AdminPostsComponent } from './admin-posts/admin-posts.component';
import { AdminPagesComponent } from './admin-pages/admin-pages.component';
import { AdminSocialLinksComponent } from './admin-social-links/admin-social-links.component';
import { AdminBrandingComponent } from './admin-branding/admin-branding.component';

const routes: Routes = [
  {
    path: 'users',
    component: AdminUsersComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'posts',
    component: AdminPostsComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'categories',
    component: AdminCategoriesComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'pages',
    component: AdminPagesComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'social-links',
    component: AdminSocialLinksComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: 'branding',
    component: AdminBrandingComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] },
  },
  { path: '', redirectTo: 'users', pathMatch: 'full' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}

