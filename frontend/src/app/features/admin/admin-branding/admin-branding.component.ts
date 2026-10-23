import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgForm, FormsModule } from '@angular/forms';
import { skip } from 'rxjs';
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
export class AdminBrandingComponent implements OnInit {
  form: Partial<SiteSettings> = {
    title: 'Toghrak X',
    logoUrl: '',
    slogan: '',
  };
  loading = false;
  saving = false;
  error?: string;
  private readonly destroyRef = inject(DestroyRef);

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
      .pipe(skip(1), takeUntilDestroyed(this.destroyRef))
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
      // The reason (e.g. not a valid image, too large) is shown as a toast by httpErrorInterceptor
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
      // The reason is shown as a toast by httpErrorInterceptor
      error: () => (this.saving = false),
    });
  }
}
