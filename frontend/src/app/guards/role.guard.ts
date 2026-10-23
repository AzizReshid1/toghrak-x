import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap, take } from 'rxjs';
import { SessionService } from '../services/session.service';

/** Only users whose role is in the route's `data.roles`; others go to the home page. */
export const roleGuard: CanActivateFn = (route) => {
  const requiredRoles = route.data['roles'] as string[] | undefined;
  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  const session = inject(SessionService);
  const router = inject(Router);
  return session.user$.pipe(
    take(1),
    switchMap((user) => (user ? of(user) : session.refreshUser())),
    map((user) => (user.roleName && requiredRoles.includes(user.roleName) ? true : router.createUrlTree(['/']))),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
