import type { Mock } from 'vitest';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  GuardResult,
  MaybeAsync,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, firstValueFrom, isObservable, of, throwError } from 'rxjs';
import { roleGuard } from './role.guard';
import { SessionService } from '../services/session.service';
import { User } from '../models/user';

describe('roleGuard', () => {
  let router: Router;
  let session: { user$: any; refreshUser: Mock };

  // Guard functions use inject(), so they must run inside an injection context
  function run(route: ActivatedRouteSnapshot): Observable<GuardResult> {
    const result: MaybeAsync<GuardResult> = TestBed.runInInjectionContext(() =>
      roleGuard(route, {} as RouterStateSnapshot)
    );
    return isObservable(result) ? result : of(result as GuardResult);
  }

  beforeEach(() => {
    session = {
      user$: of(null),
      refreshUser: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SessionService, useValue: session }],
    });

    router = TestBed.inject(Router);
  });

  it('allows access when no roles are required', async () => {
    const route = { data: {} } as unknown as ActivatedRouteSnapshot;
    const res = await firstValueFrom(run(route));
    expect(res).toBe(true);
  });

  it('allows access when user has required role', async () => {
    const user: User = { id: 1, username: 'a', email: 'a@a.com', roleName: 'ADMIN' };
    session.user$ = of(user);
    const route = { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot;

    const res = await firstValueFrom(run(route));
    expect(res).toBe(true);
  });

  it('denies access when user lacks required role', async () => {
    const user: User = { id: 1, username: 'a', email: 'a@a.com', roleName: 'EDITOR' };
    session.user$ = of(user);
    const route = { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot;

    const res = await firstValueFrom(run(route));
    const url = router.serializeUrl(res as UrlTree);
    expect(url).toBe('/');
  });

  it('refreshes user when missing and then evaluates roles', async () => {
    const user: User = { id: 1, username: 'a', email: 'a@a.com', roleName: 'ADMIN' };
    session.user$ = of(null);
    session.refreshUser.mockReturnValue(of(user));
    const route = { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot;

    const res = await firstValueFrom(run(route));
    expect(session.refreshUser).toHaveBeenCalledTimes(1);
    expect(res).toBe(true);
  });

  it('redirects to login on refresh error', async () => {
    session.user$ = of(null);
    session.refreshUser.mockReturnValue(throwError(() => new Error('boom')));
    const route = { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot;

    const res = await firstValueFrom(run(route));
    const url = router.serializeUrl(res as UrlTree);
    expect(url).toBe('/login');
  });
});
