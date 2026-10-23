import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { QuillModule } from 'ngx-quill';

import { PostsRoutingModule } from './posts-routing.module';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { PostFormComponent } from './post-form/post-form.component';

@NgModule({
    imports: [CommonModule, ReactiveFormsModule, QuillModule, PostsRoutingModule, PostDetailComponent, PostFormComponent],
})
export class PostsModule {}

