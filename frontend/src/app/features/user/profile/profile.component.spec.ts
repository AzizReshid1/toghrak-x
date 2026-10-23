import type { Mock } from 'vitest';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, of, tap, throwError } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { SessionService } from '../../../services/session.service';
import { User } from '../../../models/user';

describe('ProfileComponent', () => {
  const user: User = { id: 1, username: 'editor1', email: 'editor1@example.com', roleName: 'EDITOR' };
  let userSubject: BehaviorSubject<User | null>;
  let session: { user$: any; refreshUser: Mock };

  function create(): ProfileComponent {
    TestBed.configureTestingModule({
      imports: [ProfileComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [{ provide: SessionService, useValue: session }],
    });
    const component = TestBed.createComponent(ProfileComponent).componentInstance;
    component.ngOnInit();
    return component;
  }

  beforeEach(() => {
    userSubject = new BehaviorSubject<User | null>(null);
    session = { user$: userSubject.asObservable(), refreshUser: vi.fn() };
  });

  it('shows the user the session already has, without asking the server', () => {
    userSubject.next(user);
    const component = create();

    expect(component.user).toEqual(user);
    expect(session.refreshUser).not.toHaveBeenCalled();
  });

  it('fetches the user once when the session does not have it yet', () => {
    // Like the real service: the fetched user is also stored in user$
    session.refreshUser.mockReturnValue(of(user).pipe(tap((u) => userSubject.next(u))));
    const component = create();

    expect(component.user).toEqual(user);
    expect(component.loading).toBe(false);
    expect(session.refreshUser).toHaveBeenCalledTimes(1);
  });

  it('shows an error when the user cannot be loaded', () => {
    session.refreshUser.mockReturnValue(throwError(() => new Error('401')));
    const component = create();

    expect(component.error).toBe('Failed to load profile.');
    expect(component.loading).toBe(false);
  });
});
