import { Routes } from '@angular/router';
import { alreadyAuthGuard } from '../../guards/already-auth.guard';
import { LoginComponent } from './login/login.component';

export const AUTH_ROUTES: Routes = [
  { path: '', component: LoginComponent, canActivate: [alreadyAuthGuard] },
];
