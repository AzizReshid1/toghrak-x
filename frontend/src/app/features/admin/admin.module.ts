import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QuillModule } from 'ngx-quill';

import { AdminRoutingModule } from './admin-routing.module';
import { AdminCategoriesComponent } from './admin-categories/admin-categories.component';
import { AdminPostsComponent } from './admin-posts/admin-posts.component';
import { AdminPagesComponent } from './admin-pages/admin-pages.component';
import { AdminSocialLinksComponent } from './admin-social-links/admin-social-links.component';
import { AdminBrandingComponent } from './admin-branding/admin-branding.component';
import { AdminUsersComponent } from './admin-users/admin-users.component';

@NgModule({
    imports: [CommonModule, FormsModule, QuillModule, AdminRoutingModule, AdminUsersComponent,
        AdminCategoriesComponent,
        AdminPostsComponent,
        AdminPagesComponent,
        AdminSocialLinksComponent,
        AdminBrandingComponent],
})
export class AdminModule {}

