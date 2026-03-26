import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, finalize } from 'rxjs';
import { environment } from '../../environments/environment';
import { Category } from '../models/category';

@Injectable({ providedIn: 'root' })
export class CategoriesService {
  private base = `${environment.apiBaseUrl}/categories`;
  private categoriesSubject = new BehaviorSubject<Category[]>([]);
  categories$ = this.categoriesSubject.asObservable();
  private refreshing = false;

  constructor(private http: HttpClient) {}

  list(): Observable<Category[]> {
    return this.http.get<Category[]>(this.base).pipe(tap((cats) => this.categoriesSubject.next(cats)));
  }

  get(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.base}/${id}`);
  }

  getBySlug(slug: string): Observable<Category> {
    return this.http.get<Category>(`${this.base}/slug/${slug}`);
  }

  refresh(): void {
    if (this.refreshing) return;
    this.refreshing = true;
    this.list()
      .pipe(finalize(() => (this.refreshing = false)))
      .subscribe({
        error: () => this.categoriesSubject.next([]),
      });
  }

  create(payload: { name: string; description?: string | null }): Observable<Category> {
    return this.http.post<Category>(this.base, payload).pipe(tap(() => this.refresh()));
  }

  update(id: number, payload: { name: string; description?: string | null }): Observable<Category> {
    return this.http.put<Category>(`${this.base}/${id}`, payload).pipe(tap(() => this.refresh()));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`).pipe(tap(() => this.refresh()));
  }
}
