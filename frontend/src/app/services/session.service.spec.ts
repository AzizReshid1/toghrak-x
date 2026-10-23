import type { Mock } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { SessionService } from './session.service';
import { AuthService } from './auth.service';
import { UsersService } from './users.service';
import { User } from '../models/user';

describe('SessionService', () => {
  let authLoggedIn$: BehaviorSubject<boolean>;
  let usersGetMe$: Subject<User>;
  let usersService: { getMe: Mock };
  let session: SessionService;

  beforeEach(() => {
    authLoggedIn$ = new BehaviorSubject<boolean>(false);
    usersGetMe$ = new Subject<User>();

    usersService = { getMe: vi.fn() };
    usersService.getMe.mockReturnValue(usersGetMe$.asObservable());

    const authService = {
      isLoggedIn: () => authLoggedIn$.asObservable(),
      refresh: () => of({ token: 'access' }),
    } as unknown as AuthService;

    TestBed.configureTestingModule({
      providers: [
        SessionService,
        { provide: AuthService, useValue: authService },
        { provide: UsersService, useValue: usersService },
      ],
    });

    session = TestBed.inject(SessionService);
  });

  it('loads current user when logged in', () => {
    const user: User = { id: 1, username: 'u1', email: 'u1@example.com', roleName: 'ADMIN' };
    let latest: User | null = null;
    session.user$.subscribe((u) => (latest = u));

    authLoggedIn$.next(true);
    expect(usersService.getMe).toHaveBeenCalledTimes(1);

    usersGetMe$.next(user);
    usersGetMe$.complete();

    if (!latest) {
      expect.fail('expected user to be loaded');
      return;
    }
    expect(latest).toEqual(user);
  });

  it('clears user when logged out', () => {
    const user: User = { id: 1, username: 'u1', email: 'u1@example.com', roleName: 'ADMIN' };
    let latest: User | null = null;
    session.user$.subscribe((u) => (latest = u));

    authLoggedIn$.next(true);
    usersGetMe$.next(user);
    usersGetMe$.complete();
    if (!latest) {
      expect.fail('expected user to be loaded');
      return;
    }
    expect(latest).toEqual(user);

    authLoggedIn$.next(false);
    expect(latest).toBeNull();
  });

  it('deduplicates concurrent refreshUser calls', () => {
    const user: User = { id: 2, username: 'u2', email: 'u2@example.com', roleName: 'EDITOR' };

    let a: User | undefined;
    let b: User | undefined;

    session.refreshUser().subscribe((u) => (a = u));
    session.refreshUser().subscribe((u) => (b = u));

    expect(usersService.getMe).toHaveBeenCalledTimes(1);

    usersGetMe$.next(user);
    usersGetMe$.complete();

    expect(a).toEqual(user);
    expect(b).toEqual(user);

    // After completion, a new refresh should trigger a new request.
    usersGetMe$ = new Subject<User>();
    usersService.getMe.mockReturnValue(usersGetMe$.asObservable());

    session.refreshUser().subscribe();
    expect(usersService.getMe).toHaveBeenCalledTimes(2);
  });
});
