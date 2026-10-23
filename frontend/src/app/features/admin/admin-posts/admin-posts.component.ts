import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { PostsService } from '../../../services/posts.service';
import { PostSummary } from '../../../models/post';
import { Page } from '../../../models/page';
import { UiMessageService } from '../../../services/ui-message.service';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

/** Every post on the site, from every author, with Edit and Delete for the admin. */
@Component({
    selector: 'app-admin-posts',
    templateUrl: './admin-posts.component.html',
    imports: [RouterLink, DatePipe]
})
export class AdminPostsComponent implements OnInit, OnDestroy {
  page?: Page<PostSummary>;
  pageIndex = 0;
  readonly pageSize = 20;
  loading = false;
  error: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(private postsService: PostsService, private ui: UiMessageService) {}

  ngOnInit(): void {
    this.fetch(0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get posts(): PostSummary[] {
    return this.page?.content ?? [];
  }

  fetch(pageIndex: number): void {
    this.loading = true;
    this.pageIndex = pageIndex;
    this.postsService.list(pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.page = res;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load posts.';
          this.loading = false;
        },
      });
  }

  remove(post: PostSummary): void {
    if (!confirm(`Delete the post "${post.title}"?`)) return;
    this.postsService.delete(post.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.ui.success('Post deleted.');
          // Stay on the same page unless it just became empty
          const lastOnPage = this.posts.length === 1 && this.pageIndex > 0;
          this.fetch(lastOnPage ? this.pageIndex - 1 : this.pageIndex);
        },
        error: () => {}, // shown as a toast by HttpErrorInterceptor
      });
  }
}
