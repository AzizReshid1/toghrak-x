import { Routes } from '@angular/router';
import { authGuard } from '../../guards/auth.guard';
import { ProfileComponent } from './profile/profile.component';

export const USER_ROUTES: Routes = [
  { path: '', component: ProfileComponent, canActivate: [authGuard] },
];
