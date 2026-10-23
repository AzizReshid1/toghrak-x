import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AlreadyAuthGuard } from '../../guards/already-auth.guard';
import { LoginComponent } from './login/login.component';

const routes: Routes = [
  { path: '', component: LoginComponent, canActivate: [AlreadyAuthGuard] },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AuthRoutingModule {}

