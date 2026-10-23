import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { catchError, filter, map, of, switchMap } from 'rxjs';
import { PagesService } from '../../../services/pages.service';
import { ContentPage } from '../../../models/content-page';
import { DatePipe } from '@angular/common';

@Component({
    selector: 'app-page-view',
    templateUrl: './page-view.component.html',
    styleUrls: ['./page-view.component.css'],
    imports: [DatePipe]
})
export class PageViewComponent implements OnInit {
  page?: ContentPage;
  loading = false;
  error: string | null = null;
  private readonly destroyRef = inject(DestroyRef);

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
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((page) => {
        this.page = page ?? undefined;
        this.error = page ? null : 'Page not found.';
        this.loading = false;
      });
  }

}
