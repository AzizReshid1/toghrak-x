import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { PagesService } from '../../../services/pages.service';
import { ImagesService } from '../../../services/images.service';
import { ContentPage, UpsertContentPageRequest } from '../../../models/content-page';
import { UiMessageService } from '../../../services/ui-message.service';

// Routes owned by the Angular router — cannot be used as page slugs.
const RESERVED_SLUGS = new Set([
  'posts', 'login', 'profile', 'admin', 'page', 'search', 'home',
]);

@Component({
    selector: 'app-admin-pages',
    templateUrl: './admin-pages.component.html',
    standalone: false
})
export class AdminPagesComponent implements OnInit, OnDestroy {
  pages: ContentPage[] = [];
  loading = false;
  saving = false;
  error: string | null = null;

  current: UpsertContentPageRequest = {
    slug: '',
    title: '',
    content: '',
    images: [],
  };
  uploading = false;

  private destroy$ = new Subject<void>();

  constructor(
    private pageService: PagesService,
    private imagesService: ImagesService,
    private ui: UiMessageService
  ) {}

  ngOnInit(): void {
    this.fetch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  fetch(): void {
    this.loading = true;
    this.pageService.list()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pages) => {
          this.pages = pages;
          if (!this.current.slug && this.pages.length) {
            this.edit(this.pages[0]);
          }
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load pages.';
          this.loading = false;
        },
      });
  }

  edit(page: ContentPage): void {
    this.current = { slug: page.slug, title: page.title, content: page.content, images: page.images || [] };
  }

  newPage(): void {
    this.current = { slug: '', title: '', content: '', images: [] };
    this.ui.info('Drafting a new page. Set slug/title/content, then click Save.');
  }

  onSlugBlur(): void {
    if (this.current.slug) {
      this.current.slug = this.normalizeSlug(this.current.slug);
    }
  }

  private normalizeSlug(slug: string): string {
    return slug ? slug.trim().toLowerCase().replace(/\s+/g, '-') : '';
  }

  save(): void {
    if (this.saving) return;
    const normalized = this.normalizeSlug(this.current.slug);
    if (!normalized || !this.current.title.trim()) {
      this.ui.error('Slug and title are required.');
      return;
    }
    if (RESERVED_SLUGS.has(normalized)) {
      this.ui.error(`"${normalized}" is a reserved route and cannot be used as a page slug.`);
      return;
    }
    this.current.slug = normalized;
    this.saving = true;
    const exists = this.pages.find((p) => p.slug === normalized);
    const req$ = exists
      ? this.pageService.update(exists.slug, this.current)
      : this.pageService.create(this.current);
    req$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.fetch();
        this.ui.success(exists ? 'Page updated.' : 'Page created.');
      },
      // The reason is shown as a toast by HttpErrorInterceptor
      error: () => (this.saving = false),
    });
  }

  remove(slug: string): void {
    if (!confirm('Delete this page?')) return;
    this.pageService.delete(slug)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.ui.success('Page deleted.');
          this.fetch();
        },
        error: () => {}, // shown as a toast by HttpErrorInterceptor
      });
  }

  addImageUrl(url: string): void {
    if (!url) return;
    const trimmed = url.trim();
    if (!trimmed) return;
    const images = this.current.images || [];
    this.current = { ...this.current, images: [...images, trimmed] };
  }

  removeImage(url: string): void {
    const images = (this.current.images || []).filter((u) => u !== url);
    this.current = { ...this.current, images };
  }

  onImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.uploading = true;
    this.imagesService.upload(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (resp) => {
          this.addImageUrl(resp.imageUrl);
          this.uploading = false;
        },
        // The reason (e.g. not a valid image, too large) is shown as a toast by HttpErrorInterceptor
        error: () => (this.uploading = false),
      });
  }
}
