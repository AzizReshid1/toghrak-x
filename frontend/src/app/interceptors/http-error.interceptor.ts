import { Injectable } from '@angular/core';
import {
  HttpContextToken,
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { UiMessageService } from '../services/ui-message.service';

/** Set on a request to handle its errors silently (e.g. fire-and-forget calls). */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

/**
 * Shows one toast for a failed request, with the server's own explanation (RFC 9457 ProblemDetail
 * `detail`, plus the per-field `errors` for validation failures).
 *
 * - Failed saves, deletes and uploads (POST/PUT/PATCH/DELETE): always shown here, so screens do
 *   not need their own error boxes.
 * - Failed page loads (GET): the page shows its own "not found" / "could not load" state, so only
 *   connection problems and server errors are shown here.
 * - 401: handled by AuthInterceptor (token refresh, redirect to login); only a failed login is shown.
 */
@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  constructor(private ui: UiMessageService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        if (!req.context.get(SKIP_ERROR_TOAST)) {
          const text = this.messageFor(req, err);
          if (text) {
            this.ui.error(text);
          }
        }
        return throwError(() => err);
      })
    );
  }

  private messageFor(req: HttpRequest<any>, err: HttpErrorResponse): string | null {
    if (err.status === 0) {
      return 'Cannot reach the server. Check your connection and that the backend is running.';
    }
    if (err.status >= 500) {
      return 'Server error. Please try again.';
    }
    if (err.status === 401) {
      return req.url.includes('/auth/login') ? 'Wrong username or password.' : null;
    }
    if (req.method === 'GET') {
      return null;
    }
    return this.serverDetail(err) ?? this.fallback(err.status);
  }

  // ProblemDetail body: { detail, errors?: { field: message } }
  private serverDetail(err: HttpErrorResponse): string | null {
    const body = err.error;
    if (!body || typeof body !== 'object') {
      return null;
    }
    const fieldErrors = body.errors && typeof body.errors === 'object' ? Object.values(body.errors) : [];
    if (fieldErrors.length) {
      return fieldErrors.join(' ');
    }
    return typeof body.detail === 'string' && body.detail ? body.detail : null;
  }

  private fallback(status: number): string {
    switch (status) {
      case 403:
        return 'You do not have permission to do that.';
      case 404:
        return 'It no longer exists. Refresh the page and try again.';
      case 409:
        return 'That conflicts with existing data.';
      case 413:
        return 'The file is too large.';
      case 429:
        return 'Too many requests. Please wait a minute and try again.';
      default:
        return 'Something went wrong. Please try again.';
    }
  }
}
