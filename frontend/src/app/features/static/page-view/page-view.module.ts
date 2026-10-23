import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PageViewComponent } from './page-view.component';
import { PageViewRoutingModule } from './page-view-routing.module';

@NgModule({
    imports: [CommonModule, PageViewRoutingModule, PageViewComponent],
})
export class PageViewModule {}

