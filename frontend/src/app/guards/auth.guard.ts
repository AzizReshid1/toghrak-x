import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Logged-in users only; others are sent to /login and come back here after logging in. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toLogin = router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });

  return auth.ensureToken().pipe(
    map((token) => (token ? true : toLogin)),
    catchError(() => of(toLogin))
  );
};
