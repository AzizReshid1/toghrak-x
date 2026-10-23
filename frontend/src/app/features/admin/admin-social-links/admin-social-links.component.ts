import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { SocialLinksService } from '../../../services/social-links.service';
import { SocialLink } from '../../../models/social-link';
import { UiMessageService } from '../../../services/ui-message.service';

@Component({
    selector: 'app-admin-social-links',
    templateUrl: './admin-social-links.component.html',
    standalone: false
})
export class AdminSocialLinksComponent implements OnInit, OnDestroy {
  links: SocialLink[] = [];
  saving = false;
  private destroy$ = new Subject<void>();

  constructor(private socialLinks: SocialLinksService, private ui: UiMessageService) {}

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
      this.ui.error('Label and URL are required.');
      return;
    }
    this.saving = true;
    const req$ = link.id
      ? this.socialLinks.update(link.id, link)
      : this.socialLinks.create(link);
    req$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.ui.success('Link saved.');
      },
      // The reason is shown as a toast by HttpErrorInterceptor
      error: () => (this.saving = false),
    });
  }

  remove(link: SocialLink) {
    if (!confirm('Delete this link?')) return;
    if (!link.id) {
      this.links = this.links.filter((l) => l !== link);
      return;
    }
    this.socialLinks.delete(link.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.ui.success('Link deleted.'),
      error: () => {}, // shown as a toast by HttpErrorInterceptor
    });
  }
}
