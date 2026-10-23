import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgForm, FormsModule } from '@angular/forms';
import { Subject, skip, takeUntil } from 'rxjs';
import { SiteSettingsService } from '../../../services/site-settings.service';
import { ImagesService } from '../../../services/images.service';
import { SiteSettings } from '../../../models/site-settings';
import { UiMessageService } from '../../../services/ui-message.service';

@Component({
    selector: 'app-admin-branding',
    templateUrl: './admin-branding.component.html',
    styleUrls: ['./admin-branding.component.css'],
    imports: [FormsModule]
})
export class AdminBrandingComponent implements OnInit, OnDestroy {
  form: Partial<SiteSettings> = {
    title: 'Toghrak X',
    logoUrl: '',
    slogan: '',
  };
  loading = false;
  saving = false;
  error?: string;
  private destroy$ = new Subject<void>();

  constructor(
    private settings: SiteSettingsService,
    private images: ImagesService,
    private ui: UiMessageService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    // skip(1) skips the BehaviorSubject's initial null so loading stays true
    // until the HTTP response (or error) actually arrives.
    this.settings.settings$
      .pipe(skip(1), takeUntil(this.destroy$))
      .subscribe((s) => {
        if (s) {
          this.form.title = s.title || 'Toghrak X';
          this.form.logoUrl = s.logoUrl || '';
          this.form.slogan = s.slogan || '';
          this.error = undefined;
        } else {
          this.error = 'Failed to load branding settings.';
        }
        this.loading = false;
      });
    this.settings.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.saving = true;
    this.images.upload(file).subscribe({
      next: (res) => {
        this.form.logoUrl = res.imageUrl;
        this.ui.success('Logo uploaded. Remember to save changes.');
        this.saving = false;
      },
      // The reason (e.g. not a valid image, too large) is shown as a toast by HttpErrorInterceptor
      error: () => (this.saving = false),
    });
  }

  save(formRef: NgForm) {
    if (formRef.invalid) return;
    this.saving = true;
    this.settings.update(this.form).subscribe({
      next: () => {
        this.ui.success('Branding updated.');
        this.saving = false;
      },
      // The reason is shown as a toast by HttpErrorInterceptor
      error: () => (this.saving = false),
    });
  }
}
