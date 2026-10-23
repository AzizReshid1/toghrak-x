import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject, catchError, filter, map, of, switchMap, takeUntil } from 'rxjs';
import { PagesService } from '../../../services/pages.service';
import { ContentPage } from '../../../models/content-page';

@Component({
    selector: 'app-page-view',
    templateUrl: './page-view.component.html',
    styleUrls: ['./page-view.component.css'],
    standalone: false
})
export class PageViewComponent implements OnInit, OnDestroy {
  page?: ContentPage;
  loading = false;
  error: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(private route: ActivatedRoute, private pageService: PagesService) {}

  ngOnInit(): void {
    // switchMap cancels the previous request when the reader moves to another page, so a slow
    // response can never replace the page that is now on screen.
    this.route.paramMap
      .pipe(
        map((params) => params.get('slug')),
        filter((slug): slug is string => !!slug),
        switchMap((slug) => {
          this.loading = true;
          this.error = null;
          return this.pageService.get(slug).pipe(catchError(() => of(null)));
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((page) => {
        this.page = page ?? undefined;
        this.error = page ? null : 'Page not found.';
        this.loading = false;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
