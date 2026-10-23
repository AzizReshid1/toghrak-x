import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Page } from '../models/page';
import { SKIP_ERROR_TOAST } from '../interceptors/http-error.interceptor';
import {
  PostResponse,
  PostSummary,
  CreatePostRequest,
  UpdatePostRequest,
} from '../models/post';

@Injectable({ providedIn: 'root' })
export class PostsService {
  private base = `${environment.apiBaseUrl}/posts`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 10, categoryId?: number): Observable<Page<PostSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (categoryId !== undefined && categoryId !== null) {
      params = params.set('categoryId', categoryId);
    }
    return this.http.get<Page<PostSummary>>(this.base, { params });
  }

  get(id: number): Observable<PostResponse> {
    return this.http.get<PostResponse>(`${this.base}/${id}`);
  }

  getBySlug(slug: string): Observable<PostResponse> {
    return this.http.get<PostResponse>(`${this.base}/slug/${slug}`);
  }

  /** Counts one view of a post. Kept separate from getBySlug so that GET requests never change data. */
  recordView(id: number): Observable<void> {
    // Background call: a failed view count is never shown to the reader
    return this.http.post<void>(`${this.base}/${id}/view`, null, {
      context: new HttpContext().set(SKIP_ERROR_TOAST, true),
    });
  }

  popular(limit = 6): Observable<PostSummary[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<PostSummary[]>(`${this.base}/popular`, { params });
  }

  create(payload: CreatePostRequest): Observable<PostResponse> {
    return this.http.post<PostResponse>(this.base, payload);
  }

  update(id: number, payload: UpdatePostRequest): Observable<PostResponse> {
    return this.http.put<PostResponse>(`${this.base}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
