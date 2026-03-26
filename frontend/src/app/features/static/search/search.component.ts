import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of, takeUntil, switchMap, catchError } from 'rxjs';
import { SearchService } from '../../../services/search.service';
import { SearchResult } from '../../../models/search-result';

@Component({
    selector: 'app-search',
    templateUrl: './search.component.html',
    styleUrls: ['./search.component.css'],
    standalone: false
})
export class SearchComponent implements OnInit, OnDestroy {
  query = '';
  results: SearchResult[] = [];
  loading = false;
  error: string | null = null;
  hasSearched = false;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private searchService: SearchService
  ) {}

  ngOnInit(): void {
    // Pipe queryParamMap directly into switchMap — no intermediate Subject needed.
    // This avoids the race condition where Subject.next() fires before the switchMap
    // pipeline has subscribed, silently dropping the first search on page load.
    this.route.queryParamMap
      .pipe(
        switchMap((params) => {
          const q = params.get('q') ?? '';
          this.query = q;

          if (!q.trim()) {
            this.results = [];
            this.hasSearched = false;
            this.loading = false;
            return of([] as SearchResult[]);
          }

          this.hasSearched = true;
          this.loading = true;
          this.error = null;

          // switchMap automatically cancels any in-flight HTTP request when a new
          // query arrives, preventing out-of-order results and dangling subscriptions.
          return this.searchService.search(q).pipe(
            catchError(() => {
              this.error = 'Search failed. Please try again.';
              return of([] as SearchResult[]);
            })
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((res) => {
        this.results = res;
        this.loading = false;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Navigate with the updated query — this updates the URL and triggers
  // queryParamMap, which fires the search. URL and results are always in sync.
  runSearch(): void {
    const q = this.query.trim();
    this.router.navigate(['/search'], {
      queryParams: { q: q || null },
      replaceUrl: true
    });
  }
}
