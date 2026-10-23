import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject, combineLatest, takeUntil } from 'rxjs';
import { PostsService } from '../../../services/posts.service';
import { PostSummary } from '../../../models/post';
import { Page } from '../../../models/page';
import { User } from '../../../models/user';
import { CategoriesService } from '../../../services/categories.service';
import { Category } from '../../../models/category';
import { SessionService } from '../../../services/session.service';

@Component({
    selector: 'app-posts-list',
    templateUrl: './posts-list.component.html',
    styleUrls: ['./posts-list.component.css'],
    standalone: false
})
export class PostsListComponent implements OnInit, OnDestroy {
  page?: Page<PostSummary>;
  loading = false;
  error: string | null = null;
  popular: PostSummary[] = [];
  popularLoading = false;
  currentPage = 0;
  // Matches the layout: 1 lead story + 2 side stories + 6 cards. A bigger page would hide posts.
  readonly pageSize = 9;
  isLoggedIn$: Observable<boolean>;
  currentUser?: User;
  isAdmin = false;
  currentCategoryId?: number;
  currentCategorySlug?: string | null;
  private destroy$ = new Subject<void>();

  constructor(
    private postsService: PostsService,
    private session: SessionService,
    private categoriesService: CategoriesService,
    private route: ActivatedRoute,
    public router: Router
  ) {
    this.isLoggedIn$ = this.session.isLoggedIn$;
  }

  get posts(): PostSummary[] {
    return this.page?.content ?? [];
  }

  ngOnInit(): void {
    this.session.user$.pipe(takeUntil(this.destroy$)).subscribe((user) => (this.currentUser = user ?? undefined));
    this.session.isAdmin$.pipe(takeUntil(this.destroy$)).subscribe((isAdmin) => (this.isAdmin = isAdmin));

    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([params, q]) => {
        const slug = params.get('slug');
        const pageParam = q.get('page');
        const pageNum = pageParam ? Number(pageParam) : 0;

        if (slug) {
          this.currentCategorySlug = slug;
          this.resolveCategoryBySlug(slug, pageNum);
        } else {
          const categoryIdParam = q.get('category');
          const categoryId = categoryIdParam ? Number(categoryIdParam) : undefined;
          this.currentCategorySlug = null;
          this.currentCategoryId = categoryId;
          this.currentPage = pageNum;
          this.load(this.currentPage, categoryId);
        }
      });

    this.loadPopular();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  canEdit(post: PostSummary): boolean {
    if (this.isAdmin) return true;
    // Compare ids, not usernames: a username can change, the id cannot
    return !!this.currentUser && post.authorId === this.currentUser.id;
  }

  load(page: number, categoryId: number | undefined = this.currentCategoryId) {
    this.currentCategoryId = categoryId;
    this.currentPage = page;
    this.loading = true;
    this.error = null;
    this.postsService.list(page, this.pageSize, categoryId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: Page<PostSummary>) => {
          this.page = res;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load posts.';
          this.loading = false;
        },
      });
  }

  clearCategory() {
    this.router.navigate(['/'], { queryParams: { page: 0 } });
  }

  goToPage(page: number) {
    if (this.currentCategorySlug) {
      this.router.navigate(['/category', this.currentCategorySlug], {
        queryParams: { page },
      });
    } else {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { page, category: this.currentCategoryId ?? null },
        queryParamsHandling: 'merge',
      });
    }
  }

  private loadPopular() {
    this.popularLoading = true;
    this.postsService.popular(6)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.popular = res;
          this.popularLoading = false;
        },
        error: () => {
          this.popular = [];
          this.popularLoading = false;
        },
      });
  }

  private resolveCategoryBySlug(slug: string, pageNum: number) {
    this.categoriesService.getBySlug(slug)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
      next: (cat: Category) => {
        this.currentCategoryId = cat.id;
        this.currentPage = pageNum;
        this.load(pageNum, cat.id);
      },
      error: (err: HttpErrorResponse) => {
        this.page = undefined;
        if (err.status === 404) {
          // Unknown category: show the 404 page but keep the address, so the broken link stays visible
          this.router.navigate(['/not-found'], { skipLocationChange: true });
        } else {
          this.error = 'Failed to load posts.';
        }
      },
    });
  }
}
