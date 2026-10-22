import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
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
  /** Trusted HTML produced from post.content — bypasses Angular's redundant
   *  sanitizer because the content is already sanitized server-side by Jsoup. */
  safeContent!: SafeHtml;
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
    private sanitizer: DomSanitizer,
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
        // Fire-and-forget: a failed view count must never affect reading the post.
        this.posts.recordView(res.id).subscribe({ error: () => {} });
        this.safeContent = this.sanitizer.bypassSecurityTrustHtml(
          this.formatContent(res.content ?? '')
        );
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

  /**
   * Converts plain-text post content (legacy posts saved before the backend
   * applied HTML wrapping) into safe HTML for [innerHTML] rendering.
   * Posts that already contain block-level tags are returned unchanged.
   */
  private formatContent(raw: string): string {
    if (!raw.trim()) return '';
    // If the content already has block-level tags it was saved as HTML — leave it.
    if (/<(p|div|h[1-6]|ul|ol|blockquote|pre|table)[\s/>]/i.test(raw)) {
      return raw;
    }
    // Plain-text path: split on blank lines → <p> per paragraph, \n → <br>.
    return raw
      .trim()
      .split(/\r?\n[ \t]*\r?\n/)
      .filter(block => block.trim().length > 0)
      .map(block => `<p>${block.trim().replace(/\r?\n/g, '<br>')}</p>`)
      .join('');
  }

  private updateOwnershipFlag(): void {
    if (!this.post || !this.currentUser) {
      this.isOwner = false;
      return;
    }
    this.isOwner = this.post.authorUsername === this.currentUser.username;
  }
}
