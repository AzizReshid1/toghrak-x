import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { SocialLinksService } from '../../../services/social-links.service';
import { SocialLink } from '../../../models/social-link';

@Component({
    selector: 'app-admin-social-links',
    templateUrl: './admin-social-links.component.html',
    standalone: false
})
export class AdminSocialLinksComponent implements OnInit, OnDestroy {
  links: SocialLink[] = [];
  saving = false;
  error: string | null = null;
  info: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(private socialLinks: SocialLinksService) {}

  ngOnInit(): void {
    this.socialLinks.links$
      .pipe(takeUntil(this.destroy$))
      .subscribe((links) => (this.links = [...links]));
    this.socialLinks.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  add() {
    this.links = [...this.links, { label: 'New', url: 'https://', icon: '' }];
  }

  save(link: SocialLink) {
    if (!link.url || !link.label) {
      this.error = 'Label and URL are required.';
      return;
    }
    this.error = null;
    this.info = null;
    this.saving = true;
    const req$ = link.id
      ? this.socialLinks.update(link.id, link)
      : this.socialLinks.create(link);
    req$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.info = 'Link saved.';
      },
      error: () => {
        this.error = 'Failed to save link.';
        this.saving = false;
      },
    });
  }

  remove(link: SocialLink) {
    this.info = null;
    this.error = null;
    if (!confirm('Delete this link?')) return;
    if (!link.id) {
      this.links = this.links.filter((l) => l !== link);
      return;
    }
    this.socialLinks.delete(link.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => (this.info = 'Link deleted.'),
      error: () => (this.error = 'Failed to delete link.'),
    });
  }
}
