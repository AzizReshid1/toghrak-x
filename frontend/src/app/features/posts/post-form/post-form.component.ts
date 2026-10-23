import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, forkJoin, takeUntil } from 'rxjs';
import { CategoriesService } from '../../../services/categories.service';
import { PostsService } from '../../../services/posts.service';
import { ImagesService } from '../../../services/images.service';
import { Category } from '../../../models/category';
import { UiMessageService } from '../../../services/ui-message.service';
import {
  CreatePostRequest,
  UpdatePostRequest,
  PostResponse,
} from '../../../models/post';

@Component({
    selector: 'app-post-form',
    templateUrl: './post-form.component.html',
    styleUrls: ['./post-form.component.css'],
    standalone: false
})
export class PostFormComponent implements OnInit, OnDestroy {
  isEdit = false;
  postId?: number;
  categories: Category[] = [];
  loading = false;
  saving = false;
  error: string | null = null; // only for a post or categories that could not be loaded
  imageUrls: string[] = [];
  uploadingGallery = false;

  form = this.fb.group({
    title: ['', Validators.required],
    content: ['', Validators.required],
    categoryId: [null as number | null, Validators.required],
    coverImage: [''],
  });

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private categoriesService: CategoriesService,
    private postsService: PostsService,
    private imagesService: ImagesService,
    private ui: UiMessageService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.postId = Number(id);
      this.loadPost(this.postId);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCategories(): void {
    this.categoriesService.list()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cats) => (this.categories = cats),
        error: () => (this.error = 'Failed to load categories.'),
      });
  }

  loadPost(id: number): void {
    this.loading = true;
    this.postsService.get(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (p: PostResponse) => {
          this.form.patchValue({
            title: p.title,
            content: p.content,
            categoryId: p.categoryId ?? null,
            coverImage: p.coverImage || '',
          });
          this.imageUrls = [...(p.imageUrls || [])];
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load post.';
          this.loading = false;
        },
      });
  }

  submit(): void {
    if (this.saving) return;
    if (this.form.invalid) {
      this.ui.error('Please fill in title, content, and choose a category.');
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;

    // getRawValue() returns typed values and includes disabled controls,
    // avoiding the unsafe `as any` cast on form.value.
    const raw = this.form.getRawValue();
    const payload: CreatePostRequest | UpdatePostRequest = {
      title: raw.title ?? '',
      content: raw.content ?? '',
      categoryId: raw.categoryId as number,
      coverImage: raw.coverImage || null,
      imageUrls: this.imageUrls,
    };

    const request$ =
      this.isEdit && this.postId
        ? this.postsService.update(this.postId, payload as UpdatePostRequest)
        : this.postsService.create(payload as CreatePostRequest);

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => {
        // The toast stays visible on the next page, so there is no need to wait before navigating
        this.ui.success(this.isEdit ? 'Post updated successfully.' : 'Post published successfully.');
        this.saving = false;
        this.router.navigate(['/posts', res.slug]);
      },
      // The reason (e.g. title too long) is shown as a toast by HttpErrorInterceptor
      error: () => (this.saving = false),
    });
  }

  onCoverSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.imagesService.upload(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (resp) => this.form.patchValue({ coverImage: resp.imageUrl }),
        // The reason (e.g. not a valid image, too large) is shown as a toast by HttpErrorInterceptor
        error: () => {},
      });
  }

  onGallerySelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const files = Array.from(input.files);
    this.uploadingGallery = true;

    // forkJoin runs all uploads in parallel and emits once all complete,
    // or errors immediately if any single upload fails — no counter race condition.
    forkJoin(files.map((file) => this.imagesService.upload(file)))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (responses) => {
          this.imageUrls = [...this.imageUrls, ...responses.map((r) => r.imageUrl)];
          this.uploadingGallery = false;
        },
        // The reason is shown as a toast by HttpErrorInterceptor
        error: () => (this.uploadingGallery = false),
      });
  }

  removeGalleryImage(url: string): void {
    this.imageUrls = this.imageUrls.filter((u) => u !== url);
  }
}
