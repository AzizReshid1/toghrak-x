import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, of, switchMap, take, takeUntil } from 'rxjs';
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
    this.session.user$
      .pipe(takeUntil(this.destroy$))
      .subscribe((u) => {
        if (u) {
          this.user = u;
          this.loading = false;
        }
      });

    this.session.user$
      .pipe(
        take(1),
        switchMap((u) => (u ? of(u) : this.session.refreshUser())),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (u) => {
          this.user = u;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load profile.';
          this.loading = false;
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
