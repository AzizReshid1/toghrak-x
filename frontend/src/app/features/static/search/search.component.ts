import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, of, switchMap, catchError } from 'rxjs';
import { SearchService } from '../../../services/search.service';
import { SearchResult } from '../../../models/search-result';
import { FormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';

@Component({
    selector: 'app-search',
    templateUrl: './search.component.html',
    styleUrls: ['./search.component.css'],
    imports: [FormsModule, NgTemplateOutlet, RouterLink]
})
export class SearchComponent implements OnInit {
  query = '';
  results: SearchResult[] = [];
  loading = false;
  error: string | null = null;
  hasSearched = false;

  private readonly destroyRef = inject(DestroyRef);

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
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((res) => {
        this.results = res;
        this.loading = false;
      });
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
