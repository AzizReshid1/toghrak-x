import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Keeps logged-in users away from the login page by sending them home. */
export const alreadyAuthGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const home = inject(Router).createUrlTree(['/']);

  if (auth.getToken()) {
    return home;
  }
  return auth.ensureToken().pipe(
    map((token) => (token ? home : true)),
    catchError(() => of(true))
  );
};
