import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CategoriesService } from '../../../services/categories.service';
import { Category } from '../../../models/category';
import { UiMessageService } from '../../../services/ui-message.service';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-admin-categories',
    templateUrl: './admin-categories.component.html',
    imports: [FormsModule]
})
export class AdminCategoriesComponent implements OnInit {
  categories: Category[] = [];
  loading = false;
  saving = false;
  error: string | null = null;

  form: { name: string; description: string } = {
    name: '',
    description: '',
  };
  editingId: number | null = null;

  private readonly destroyRef = inject(DestroyRef);

  constructor(private categoriesService: CategoriesService, private ui: UiMessageService) {}

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading = true;
    // list() already updates categories$ via tap — no extra refresh() needed here.
    this.categoriesService.list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cats) => {
          this.categories = cats;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load categories.';
          this.loading = false;
        },
      });
  }

  save(): void {
    if (this.saving || !this.form.name.trim()) return;
    this.saving = true;
    const payload = {
      name: this.form.name.trim(),
      description: this.form.description.trim(),
    };

    const isEditing = this.editingId !== null;
    // create/update call refresh() internally via tap — no manual refresh needed.
    const request$ = isEditing && this.editingId !== null
      ? this.categoriesService.update(this.editingId, payload)
      : this.categoriesService.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.form = { name: '', description: '' };
        this.editingId = null;
        this.fetch();
        this.saving = false;
        this.ui.success(isEditing ? 'Category updated.' : 'Category created.');
      },
      // The reason (e.g. name already taken) is shown as a toast by httpErrorInterceptor
      error: () => (this.saving = false),
    });
  }

  remove(id: number): void {
    if (!confirm('Delete this category?')) return;
    // delete calls refresh() internally via tap — no manual refresh needed.
    this.categoriesService.delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.fetch();
          this.ui.success('Category deleted.');
        },
        // The reason (e.g. posts still use this category) is shown as a toast by httpErrorInterceptor
        error: () => {},
      });
  }

  startEdit(cat: Category): void {
    this.editingId = cat.id;
    this.form = { name: cat.name, description: cat.description || '' };
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form = { name: '', description: '' };
  }
}
