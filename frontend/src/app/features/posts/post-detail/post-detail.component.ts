import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { PostsService } from '../../../services/posts.service';
import { PostResponse } from '../../../models/post';
import { Subject, takeUntil } from 'rxjs';
import { User } from '../../../models/user';
import { SessionService } from '../../../services/session.service';
import { NavHighlightService } from '../../../services/nav-highlight.service';

@Component({
    selector: 'app-post-detail',
    templateUrl: './post-detail.component.html',
    styleUrls: ['./post-detail.component.css'],
    standalone: false
})
export class PostDetailComponent implements OnInit, OnDestroy {
  post?: PostResponse;
  loading = false;
  error: string | null = null;
  deleting = false;
  currentUser?: User;
  isAdmin = false;
  isOwner = false;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private posts: PostsService,
    private session: SessionService,
    private navHighlight: NavHighlightService
  ) {}

  ngOnInit(): void {
    this.session.user$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.currentUser = user ?? undefined;
      this.isAdmin = user?.roleName === 'ADMIN';
      this.updateOwnershipFlag();
    });

    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.router.navigateByUrl('/');
      return;
    }
    this.fetch(slug);
  }

  ngOnDestroy(): void {
    this.navHighlight.clear();
    this.destroy$.next();
    this.destroy$.complete();
  }

  goBack(): void {
    this.location.back();
  }

  fetch(slug: string): void {
    this.loading = true;
    this.error = null;
    this.posts.getBySlug(slug).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => {
        this.post = res;
        this.loading = false;
        this.navHighlight.setCategory({
          id: res.categoryId ?? null,
          name: res.categoryName ?? null,
        });
        this.updateOwnershipFlag();
      },
      error: () => {
        this.error = 'Post not found.';
        this.loading = false;
        this.navHighlight.clear();
      },
    });
  }

  deletePost(): void {
    if (!this.post || this.deleting) return;
    if (!confirm('Delete this post?')) return;
    this.deleting = true;
    this.posts.delete(this.post.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: () => {
        this.error = 'Failed to delete post.';
        this.deleting = false;
      },
    });
  }

  private updateOwnershipFlag(): void {
    if (!this.post || !this.currentUser) {
      this.isOwner = false;
      return;
    }
    this.isOwner = this.post.authorUsername === this.currentUser.username;
  }
}
