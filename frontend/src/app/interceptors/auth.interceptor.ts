import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse,
} from '@angular/common/http';
import { Observable, catchError, throwError, switchMap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService, private router: Router) {}

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    // The token is only for our own API. Any other URL (another site, a CDN) must never receive it.
    if (!this.isApiRequest(req.url)) {
      return next.handle(req);
    }

    // Skip auth endpoints from refresh handling
    const isAuthEndpoint =
      req.url.includes('/auth/login') ||
      req.url.includes('/auth/refresh') ||
      req.url.includes('/auth/logout');

    const token = this.auth.getToken();
    const authReq = token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

    return next.handle(authReq).pipe(
      catchError((err: HttpErrorResponse) => {
        if (!isAuthEndpoint && err.status === 401) {
          return this.handle401(authReq, next);
        }
        // 403 means "logged in, but not allowed to do this": the session stays, and
        // HttpErrorInterceptor shows the reason. Only a failed refresh (in handle401) logs out.
        return throwError(() => err);
      })
    );
  }

  private isApiRequest(url: string): boolean {
    const base = environment.apiBaseUrl;
    return url === base || url.startsWith(`${base}/`);
  }

  // AuthService.refresh() already shares one refresh request between all callers, so any number
  // of simultaneous 401s cause a single refresh; each failed request is then retried with the new token.
  private handle401(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return this.auth.refresh().pipe(
      catchError((refreshErr) => {
        // The session cannot be renewed. With several requests failing at once, only the first one
        // still finds a token, so the logout runs once; navigating to /login again is a no-op.
        if (this.auth.getToken()) {
          this.auth.logout();
        }
        this.router.navigateByUrl('/login');
        return throwError(() => refreshErr);
      }),
      switchMap((res) => next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${res.token}` } })))
    );
  }
}
