import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PageViewComponent } from './page-view.component';

const routes: Routes = [{ path: ':slug', component: PageViewComponent }];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PageViewRoutingModule {}

