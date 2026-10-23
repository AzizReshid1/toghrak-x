import { Component, OnDestroy, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Location, DatePipe } from '@angular/common';
import { PostsService } from '../../../services/posts.service';
import { PostResponse } from '../../../models/post';
import { catchError, filter, map, of, switchMap } from 'rxjs';
import { User } from '../../../models/user';
import { SessionService } from '../../../services/session.service';
import { NavHighlightService } from '../../../services/nav-highlight.service';

@Component({
    selector: 'app-post-detail',
    templateUrl: './post-detail.component.html',
    styleUrls: ['./post-detail.component.css'],
    imports: [RouterLink, DatePipe]
})
export class PostDetailComponent implements OnInit, OnDestroy {
  post?: PostResponse;
  /** Post body as HTML, bound with [innerHTML] so Angular sanitizes it in the browser too.
   *  The backend already cleans it with Jsoup; two independent locks are safer than one. */
  contentHtml = '';
  loading = false;
  error: string | null = null;
  deleting = false;
  currentUser?: User;
  isAdmin = false;
  isOwner = false;
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private posts: PostsService,
    private session: SessionService,
    private navHighlight: NavHighlightService
  ) {}

  ngOnInit(): void {
    this.session.user$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((user) => {
      this.currentUser = user ?? undefined;
      this.updateOwnershipFlag();
    });
    this.session.isAdmin$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((isAdmin) => (this.isAdmin = isAdmin));

    // React to every slug, not just the first one: Angular reuses this component when only the
    // slug changes (e.g. a link from one post to another). switchMap cancels the previous request,
    // so a slow response can never replace the post that is now on screen.
    this.route.paramMap
      .pipe(
        map((params) => params.get('slug')),
        filter((slug): slug is string => !!slug),
        switchMap((slug) => {
          this.loading = true;
          this.error = null;
          return this.posts.getBySlug(slug).pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((post) => (post ? this.show(post) : this.showNotFound()));
  }

  ngOnDestroy(): void {
    this.navHighlight.clear();
  }

  goBack(): void {
    this.location.back();
  }

  private show(res: PostResponse): void {
    this.post = res;
    // Fire-and-forget: a failed view count must never affect reading the post.
    this.posts.recordView(res.id).subscribe({ error: () => {} });
    this.contentHtml = this.formatContent(res.content ?? '');
    this.loading = false;
    this.navHighlight.setCategory({
      id: res.categoryId ?? null,
      name: res.categoryName ?? null,
    });
    this.updateOwnershipFlag();
  }

  private showNotFound(): void {
    this.post = undefined;
    this.error = 'Post not found.';
    this.loading = false;
    this.navHighlight.clear();
  }

  deletePost(): void {
    if (!this.post || this.deleting) return;
    if (!confirm('Delete this post?')) return;
    this.deleting = true;
    this.posts.delete(this.post.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigateByUrl('/'),
      // The reason is shown as a toast by httpErrorInterceptor; the post stays on screen
      error: () => (this.deleting = false),
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
    // Compare ids, not usernames: a username can change, the id cannot
    this.isOwner = this.post.authorId === this.currentUser.id;
  }
}
