import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse,
} from '@angular/common/http';
import { Observable, catchError, throwError, switchMap, shareReplay } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private refresh$?: Observable<any>;

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

  private handle401(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.refresh$) {
      // shareReplay(1) ensures concurrent 401s share a single refresh call.
      this.refresh$ = this.auth.refresh().pipe(
        catchError((refreshErr) => {
          this.refresh$ = undefined;
          this.auth.logout();
          this.router.navigateByUrl('/login');
          return throwError(() => refreshErr);
        }),
        shareReplay(1)
      );
    }

    return this.refresh$.pipe(
      switchMap(() => {
        this.refresh$ = undefined;
        const newToken = this.auth.getToken();
        const retryReq = newToken
          ? req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } })
          : req;
        return next.handle(retryReq);
      })
    );
  }
}
