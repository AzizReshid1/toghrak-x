import { Routes } from '@angular/router';
import { PageViewComponent } from './page-view.component';

export const PAGE_VIEW_ROUTES: Routes = [{ path: ':slug', component: PageViewComponent }];
