import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SocialLinksService } from '../../../services/social-links.service';
import { SocialLink } from '../../../models/social-link';
import { UiMessageService } from '../../../services/ui-message.service';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-admin-social-links',
    templateUrl: './admin-social-links.component.html',
    imports: [FormsModule]
})
export class AdminSocialLinksComponent implements OnInit {
  links: SocialLink[] = [];
  saving = false;
  private readonly destroyRef = inject(DestroyRef);

  constructor(private socialLinks: SocialLinksService, private ui: UiMessageService) {}

  ngOnInit(): void {
    this.socialLinks.links$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((links) => (this.links = [...links]));
    this.socialLinks.load();
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
    req$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving = false;
        this.ui.success('Link saved.');
      },
      // The reason is shown as a toast by httpErrorInterceptor
      error: () => (this.saving = false),
    });
  }

  remove(link: SocialLink) {
    if (!confirm('Delete this link?')) return;
    if (!link.id) {
      this.links = this.links.filter((l) => l !== link);
      return;
    }
    this.socialLinks.delete(link.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.ui.success('Link deleted.'),
      error: () => {}, // shown as a toast by httpErrorInterceptor
    });
  }
}
