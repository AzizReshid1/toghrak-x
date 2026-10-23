import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, map, of } from 'rxjs';
import { AppComponent } from './app.component';
import { AuthService } from './services/auth.service';
import { SessionService } from './services/session.service';
import { CategoriesService } from './services/categories.service';
import { PagesService } from './services/pages.service';
import { SocialLinksService } from './services/social-links.service';
import { SiteSettingsService } from './services/site-settings.service';
import { Toast, UiMessageService } from './services/ui-message.service';
import { NavHighlightCategory, NavHighlightService } from './services/nav-highlight.service';
import { Category } from './models/category';
import { User } from './models/user';

@Component({
    template: '',
    standalone: false
})
class DummyComponent {}

describe('AppComponent', () => {
  const user: User = { id: 1, username: 'u1', email: 'u1@example.com', roleName: 'ADMIN' };
  const categories: Category[] = [
    { id: 1, name: 'Events', slug: 'events' },
    { id: 2, name: 'News', slug: 'news' },
  ];

  let toastsSubject: BehaviorSubject<Toast[]>;
  let userSubject: BehaviorSubject<User | null>;
  let loggedInSubject: BehaviorSubject<boolean>;
  let highlightSubject: BehaviorSubject<NavHighlightCategory | null>;
  let uiService: { toasts$: any; dismiss: jasmine.Spy };

  beforeEach(async () => {
    document.body.style.overflow = '';

    toastsSubject = new BehaviorSubject<Toast[]>([]);
    userSubject = new BehaviorSubject<User | null>(user);
    loggedInSubject = new BehaviorSubject<boolean>(true);
    highlightSubject = new BehaviorSubject<NavHighlightCategory | null>(null);

    uiService = {
      toasts$: toastsSubject.asObservable(),
      dismiss: jasmine
        .createSpy('dismiss')
        .and.callFake((id: number) => toastsSubject.next(toastsSubject.value.filter((t) => t.id !== id))),
    };

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        FormsModule,
        RouterTestingModule.withRoutes([
          { path: '', component: DummyComponent },
          { path: '**', component: DummyComponent },
        ]),
      ],
      declarations: [AppComponent],
      providers: [
        { provide: AuthService, useValue: { logout: () => {} } },
        {
          provide: SessionService,
          useValue: {
            isLoggedIn$: loggedInSubject.asObservable(),
            user$: userSubject.asObservable(),
            isAdmin$: userSubject.pipe(map((u) => u?.roleName === 'ADMIN')),
          },
        },
        { provide: CategoriesService, useValue: { categories$: of(categories), refresh: () => {} } },
        { provide: PagesService, useValue: { pages$: of([]), load: () => {} } },
        { provide: SocialLinksService, useValue: { links$: of([]), load: () => {} } },
        { provide: SiteSettingsService, useValue: { settings$: of(null), load: () => {} } },
        { provide: UiMessageService, useValue: uiService },
        { provide: NavHighlightService, useValue: { category$: highlightSubject.asObservable() } },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('toggles menu open/close and locks body scroll', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const toggleBtn = fixture.nativeElement.querySelector('button.menu-toggle') as HTMLButtonElement;
    toggleBtn.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.isMenuOpen).toBeTrue();
    expect(document.body.style.overflow).toBe('hidden');
    expect(fixture.nativeElement.querySelector('.menu-backdrop')).toBeTruthy();

    const closeBtn = fixture.nativeElement.querySelector('button.menu-close') as HTMLButtonElement;
    closeBtn.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.isMenuOpen).toBeFalse();
    expect(document.body.style.overflow).toBe('');
  });

  it('highlights the category from a /category/:slug address and nothing on other pages', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/category/news');
    expect(fixture.componentInstance.activeCategorySlug).toBe('news');
    expect(fixture.componentInstance.isHomeActive).toBeFalse();

    await router.navigateByUrl('/search');
    expect(fixture.componentInstance.activeCategorySlug).toBeNull();

    await router.navigateByUrl('/');
    expect(fixture.componentInstance.isHomeActive).toBeTrue();
  });

  it('highlights the category of the post being read, but not on the post editor', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/posts/some-news-story');
    highlightSubject.next({ id: 2, name: 'News' }); // reported by PostDetailComponent
    expect(fixture.componentInstance.activeCategorySlug).toBe('news');

    await router.navigateByUrl('/posts/7/edit');
    expect(fixture.componentInstance.activeCategorySlug).toBeNull();
  });

  it('closes the account menu on a click outside it or Escape, but not on a click inside', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const details = fixture.nativeElement.querySelector('details.account-menu') as HTMLDetailsElement;
    const open = () => {
      details.open = true;
      details.dispatchEvent(new Event('toggle'));
      fixture.detectChanges();
    };

    open();
    details.querySelector('.account-panel')!.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(fixture.componentInstance.isAccountMenuOpen).toBeTrue();

    document.body.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.isAccountMenuOpen).toBeFalse();
    expect(details.open).toBeFalse();

    open();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(fixture.componentInstance.isAccountMenuOpen).toBeFalse();
  });

  it('tracks account menu open/close via details toggle', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const details = fixture.nativeElement.querySelector('details.account-menu') as HTMLDetailsElement;
    expect(details).toBeTruthy();

    details.open = true;
    details.dispatchEvent(new Event('toggle'));
    fixture.detectChanges();
    expect(fixture.componentInstance.isAccountMenuOpen).toBeTrue();

    details.open = false;
    details.dispatchEvent(new Event('toggle'));
    fixture.detectChanges();
    expect(fixture.componentInstance.isAccountMenuOpen).toBeFalse();
  });

  it('closes menus when a nav link is clicked', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    // Open both menus the way a user does (writing the fields directly bypasses Angular's
    // normal update path and trips its development-mode check, NG0100)
    (fixture.nativeElement.querySelector('button.menu-toggle') as HTMLButtonElement).click();
    const details = fixture.nativeElement.querySelector('details.account-menu') as HTMLDetailsElement;
    details.open = true;
    details.dispatchEvent(new Event('toggle'));
    fixture.detectChanges();
    expect(fixture.componentInstance.isMenuOpen).toBeTrue();
    expect(fixture.componentInstance.isAccountMenuOpen).toBeTrue();
    expect(document.body.style.overflow).toBe('hidden');

    const homeLink = fixture.debugElement
      .queryAll(By.css('a.menu-item'))
      .find((de) => (de.nativeElement.textContent ?? '').trim() === 'Home');

    expect(homeLink).toBeTruthy();
    homeLink!.triggerEventHandler('click', new MouseEvent('click'));
    fixture.detectChanges();

    expect(fixture.componentInstance.isMenuOpen).toBeFalse();
    expect(fixture.componentInstance.isAccountMenuOpen).toBeFalse();
    expect(document.body.style.overflow).toBe('');
  });

  it('shows toasts and closes only the one whose close button is clicked', () => {
    const fixture = TestBed.createComponent(AppComponent);
    toastsSubject.next([
      { id: 1, kind: 'error', text: 'Boom' },
      { id: 2, kind: 'success', text: 'Saved.' },
    ]);
    fixture.detectChanges();

    const toasts = fixture.nativeElement.querySelectorAll('.toast');
    expect(toasts.length).toBe(2);
    expect(toasts[0].getAttribute('role')).toBe('alert');

    (toasts[0].querySelector('.toast-close') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(uiService.dismiss).toHaveBeenCalledOnceWith(1);
    const remaining = fixture.nativeElement.querySelectorAll('.toast');
    expect(remaining.length).toBe(1);
    expect(remaining[0].textContent).toContain('Saved.');
  });
});
