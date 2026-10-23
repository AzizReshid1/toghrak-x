import { Injectable, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, Observable, catchError, distinctUntilChanged, finalize, map, of, shareReplay, switchMap, tap } from 'rxjs';
import { AuthService } from './auth.service';
import { UsersService } from './users.service';
import { User } from '../models/user';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private userSubject = new BehaviorSubject<User | null>(null);
  readonly user$ = this.userSubject.asObservable();
  private meRequest$?: Observable<User>;

  readonly isLoggedIn$: Observable<boolean> = this.auth
    .isLoggedIn()
    .pipe(distinctUntilChanged());

  readonly isAdmin$: Observable<boolean> = this.user$.pipe(
    map((u) => u?.roleName === 'ADMIN'),
    distinctUntilChanged()
  );

  private readonly destroyRef = inject(DestroyRef);

  constructor(private auth: AuthService, private users: UsersService) {
    // Best-effort session restore: if a refresh cookie exists, acquire an access token in-memory.
    this.auth.refresh().subscribe({ next: () => {}, error: () => {} });

    this.isLoggedIn$
      .pipe(
        switchMap((loggedIn) => {
          if (!loggedIn) {
            this.userSubject.next(null);
            return of(null);
          }
          return this.fetchMeShared().pipe(
            catchError(() => {
              this.userSubject.next(null);
              return of(null);
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  refreshUser(): Observable<User> {
    return this.fetchMeShared();
  }

  private fetchMeShared(): Observable<User> {
    if (!this.meRequest$) {
      this.meRequest$ = this.users.getMe().pipe(
        tap((u) => this.userSubject.next(u)),
        finalize(() => {
          this.meRequest$ = undefined;
        }),
        shareReplay(1)
      );
    }
    return this.meRequest$;
  }

}
