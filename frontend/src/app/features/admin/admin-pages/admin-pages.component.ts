import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { PagesService } from '../../../services/pages.service';
import { ImagesService } from '../../../services/images.service';
import { ContentPage, UpsertContentPageRequest } from '../../../models/content-page';

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
  info: string | null = null;

  current: UpsertContentPageRequest = {
    slug: '',
    title: '',
    content: '',
    images: [],
  };
  uploadError: string | null = null;
  uploading = false;

  private destroy$ = new Subject<void>();

  constructor(private pageService: PagesService, private imagesService: ImagesService) {}

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
    this.info = null;
    this.error = null;
  }

  newPage(): void {
    this.current = { slug: '', title: '', content: '', images: [] };
    this.info = 'Drafting a new page. Set slug/title/content, then click Save.';
    this.error = null;
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
      this.error = 'Slug and title are required.';
      return;
    }
    if (RESERVED_SLUGS.has(normalized)) {
      this.error = `"${normalized}" is a reserved route and cannot be used as a page slug.`;
      return;
    }
    this.current.slug = normalized;
    this.info = null;
    this.saving = true;
    this.error = null;
    const exists = this.pages.find((p) => p.slug === normalized);
    const req$ = exists
      ? this.pageService.update(exists.slug, this.current)
      : this.pageService.create(this.current);
    req$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.fetch();
        this.info = exists ? 'Page updated.' : 'Page created.';
      },
      error: () => {
        this.error = 'Failed to save page.';
        this.saving = false;
      },
    });
  }

  remove(slug: string): void {
    if (!confirm('Delete this page?')) return;
    this.pageService.delete(slug)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.info = 'Page deleted.';
          this.fetch();
        },
        error: () => (this.error = 'Failed to delete page.'),
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
    this.uploadError = null;
    this.uploading = true;
    this.imagesService.upload(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (resp) => {
          this.addImageUrl(resp.imageUrl);
          this.uploading = false;
        },
        error: () => {
          this.uploadError = 'Upload failed.';
          this.uploading = false;
        },
      });
  }
}
