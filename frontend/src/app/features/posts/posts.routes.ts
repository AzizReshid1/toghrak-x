import { Routes } from '@angular/router';
import { authGuard } from '../../guards/auth.guard';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { PostFormComponent } from './post-form/post-form.component';

export const POSTS_ROUTES: Routes = [
  { path: '', redirectTo: '/', pathMatch: 'full' },
  { path: 'new', component: PostFormComponent, canActivate: [authGuard] },
  { path: ':id/edit', component: PostFormComponent, canActivate: [authGuard] },
  { path: ':slug', component: PostDetailComponent },
];
