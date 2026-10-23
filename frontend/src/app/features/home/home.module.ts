import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { HomeRoutingModule } from './home-routing.module';
import { PostsListComponent } from '../posts/posts-list/posts-list.component';

@NgModule({
    imports: [CommonModule, HomeRoutingModule, PostsListComponent],
})
export class HomeModule {}

