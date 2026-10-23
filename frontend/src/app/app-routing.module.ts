import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { NotFoundComponent } from './features/static/not-found/not-found.component';

const routes: Routes = [
  {
    path: 'posts',
    loadChildren: () =>
      import('./features/posts/posts.module').then((m) => m.PostsModule),
  },
  {
    path: 'login',
    loadChildren: () =>
      import('./features/auth/auth.module').then((m) => m.AuthModule),
  },
  {
    path: 'profile',
    loadChildren: () =>
      import('./features/user/user.module').then((m) => m.UserModule),
  },
  {
    path: 'admin',
    loadChildren: () =>
      import('./features/admin/admin.module').then((m) => m.AdminModule),
  },
  { path: 'home', redirectTo: '', pathMatch: 'full' },
  // Named 404 page that screens can show for content that does not exist (a single word like /404
  // would be taken for an old category address and redirected to /category/404).
  { path: 'not-found', component: NotFoundComponent },
  {
    path: 'search',
    loadChildren: () =>
      import('./features/static/search/search.module').then((m) => m.SearchModule),
  },
  {
    path: 'page',
    loadChildren: () =>
      import('./features/static/page-view/page-view.module').then((m) => m.PageViewModule),
  },
  {
    path: '',
    loadChildren: () =>
      import('./features/home/home.module').then((m) => m.HomeModule),
  },
  { path: '**', component: NotFoundComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { onSameUrlNavigation: 'reload' })],
  exports: [RouterModule],
})
export class AppRoutingModule {}
