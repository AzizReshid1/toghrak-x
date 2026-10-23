import { inject } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { Observable, catchError, throwError, switchMap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // The token is only for our own API. Any other URL (another site, a CDN) must never receive it.
  if (!isApiRequest(req.url)) {
    return next(req);
  }

  const auth = inject(AuthService);
  const router = inject(Router);

  // Skip auth endpoints from refresh handling
  const isAuthEndpoint =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/refresh') ||
    req.url.includes('/auth/logout');

  const token = auth.getToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (!isAuthEndpoint && err.status === 401) {
        return refreshAndRetry(authReq, next, auth, router);
      }
      // 403 means "logged in, but not allowed to do this": the session stays, and
      // httpErrorInterceptor shows the reason. Only a failed refresh (in refreshAndRetry) logs out.
      return throwError(() => err);
    })
  );
};

function isApiRequest(url: string): boolean {
  const base = environment.apiBaseUrl;
  return url === base || url.startsWith(`${base}/`);
}

// AuthService.refresh() already shares one refresh request between all callers, so any number
// of simultaneous 401s cause a single refresh; each failed request is then retried with the new token.
function refreshAndRetry(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: AuthService,
  router: Router
): Observable<HttpEvent<unknown>> {
  return auth.refresh().pipe(
    catchError((refreshErr) => {
      // The session cannot be renewed. With several requests failing at once, only the first one
      // still finds a token, so the logout runs once; navigating to /login again is a no-op.
      if (auth.getToken()) {
        auth.logout();
      }
      router.navigateByUrl('/login');
      return throwError(() => refreshErr);
    }),
    switchMap((res) => next(req.clone({ setHeaders: { Authorization: `Bearer ${res.token}` } })))
  );
}
