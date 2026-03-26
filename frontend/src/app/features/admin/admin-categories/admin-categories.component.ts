import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { CategoriesService } from '../../../services/categories.service';
import { Category } from '../../../models/category';

@Component({
    selector: 'app-admin-categories',
    templateUrl: './admin-categories.component.html',
    standalone: false
})
export class AdminCategoriesComponent implements OnInit, OnDestroy {
  categories: Category[] = [];
  loading = false;
  saving = false;
  error: string | null = null;
  info: string | null = null;

  form: { name: string; description: string } = {
    name: '',
    description: '',
  };
  editingId: number | null = null;

  private destroy$ = new Subject<void>();

  constructor(private categoriesService: CategoriesService) {}

  ngOnInit(): void {
    this.fetch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  fetch(): void {
    this.loading = true;
    // list() already updates categories$ via tap — no extra refresh() needed here.
    this.categoriesService.list()
      .pipe(takeUntil(this.destroy$))
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
    this.error = null;
    this.info = null;
    const payload = {
      name: this.form.name.trim(),
      description: this.form.description.trim(),
    };

    const isEditing = this.editingId !== null;
    // create/update call refresh() internally via tap — no manual refresh needed.
    const request$ = isEditing && this.editingId !== null
      ? this.categoriesService.update(this.editingId, payload)
      : this.categoriesService.create(payload);

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.form = { name: '', description: '' };
        this.editingId = null;
        this.fetch();
        this.saving = false;
        this.info = isEditing ? 'Category updated.' : 'Category created.';
      },
      error: () => {
        this.error = isEditing ? 'Failed to update category.' : 'Failed to create category.';
        this.saving = false;
      },
    });
  }

  remove(id: number): void {
    if (!confirm('Delete this category?')) return;
    this.error = null;
    this.info = null;
    // delete calls refresh() internally via tap — no manual refresh needed.
    this.categoriesService.delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.fetch();
          this.info = 'Category deleted.';
        },
        error: (err) => {
          // Surface backend message (e.g., if posts still exist under this category).
          this.error = err?.error?.message || 'Failed to delete category.';
        },
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
