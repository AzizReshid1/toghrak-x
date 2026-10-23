import { Routes } from '@angular/router';
import { PostsListComponent } from '../posts/posts-list/posts-list.component';

export const HOME_ROUTES: Routes = [
  { path: '', component: PostsListComponent }, // Home
  // Categories live under their own prefix (e.g. /category/events), so a category can never
  // collide with an app route such as /search or /page.
  { path: 'category/:slug', component: PostsListComponent },
  // Old category URLs (/events) from before the prefix keep working for bookmarks and shared links
  { path: ':slug', redirectTo: 'category/:slug' },
];
