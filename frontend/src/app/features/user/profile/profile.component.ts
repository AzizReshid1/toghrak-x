import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';
import { User } from '../../../models/user';
import { SessionService } from '../../../services/session.service';

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  user?: User;
  loading = false;
  error: string | null = null;

  private readonly destroyRef = inject(DestroyRef);

  constructor(private session: SessionService) {}

  ngOnInit(): void {
    this.loading = true;
    // One stream: the signed-in user from the session, fetched from the server if the session does
    // not have it yet (refreshUser() also stores it in user$, which then emits it here).
    this.session.user$
      .pipe(
        switchMap((u) => (u ? of(u) : this.session.refreshUser().pipe(catchError(() => of(null))))),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((u) => {
        this.user = u ?? undefined;
        this.error = u ? null : 'Failed to load profile.';
        this.loading = false;
      });
  }

}
