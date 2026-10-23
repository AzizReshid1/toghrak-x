import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, catchError, of, switchMap, takeUntil } from 'rxjs';
import { User } from '../../../models/user';
import { SessionService } from '../../../services/session.service';

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html',
    standalone: false
})
export class ProfileComponent implements OnInit, OnDestroy {
  user?: User;
  loading = false;
  error: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(private session: SessionService) {}

  ngOnInit(): void {
    this.loading = true;
    // One stream: the signed-in user from the session, fetched from the server if the session does
    // not have it yet (refreshUser() also stores it in user$, which then emits it here).
    this.session.user$
      .pipe(
        switchMap((u) => (u ? of(u) : this.session.refreshUser().pipe(catchError(() => of(null))))),
        takeUntil(this.destroy$)
      )
      .subscribe((u) => {
        this.user = u ?? undefined;
        this.error = u ? null : 'Failed to load profile.';
        this.loading = false;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
