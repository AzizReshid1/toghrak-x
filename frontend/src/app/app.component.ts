import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Observable, Subject, filter, takeUntil } from 'rxjs';
import { AuthService } from './services/auth.service';
import { CategoriesService } from './services/categories.service';
import { PagesService } from './services/pages.service';
import { SocialLinksService } from './services/social-links.service';
import {
  NavHighlightCategory,
  NavHighlightService,
} from './services/nav-highlight.service';
import { SocialLink } from './models/social-link';
import { User } from './models/user';
import { Category } from './models/category';
import { ContentPage } from './models/content-page';
import { SiteSettingsService } from './services/site-settings.service';
import { SessionService } from './services/session.service';
import { Toast, UiMessageService } from './services/ui-message.service';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    standalone: false
})
export class AppComponent implements OnInit, OnDestroy {
  isLoggedIn$: Observable<boolean>;
  currentUser?: User;
  isAdmin = false;
  categories: Category[] = [];
  headerSearch = '';
  footerSearch = '';
  footerPages: ContentPage[] = [];
  socialLinks: SocialLink[] = [];
  brandLogo = 'assets/tn.png';
  brandTitle = 'Toghrak X';
  brandTagline = 'Stories that matter, in one place.';
  isMenuOpen = false;
  isAccountMenuOpen = false;
  @ViewChild('accountMenu') accountMenu?: ElementRef<HTMLDetailsElement>;
  activeCategorySlug: string | null = null;
  isHomeActive = true;
  toasts$: Observable<Toast[]>;
  private destroy$ = new Subject<void>();
  private navHighlightCategory: NavHighlightCategory | null = null;

  constructor(
    private auth: AuthService,
    private session: SessionService,
    private categoriesService: CategoriesService,
    private pageService: PagesService,
    private socialLinksService: SocialLinksService,
    private siteSettingsService: SiteSettingsService,
    private ui: UiMessageService,
    private navHighlight: NavHighlightService,
    private router: Router
  ) {
    this.isLoggedIn$ = this.session.isLoggedIn$;
    this.toasts$ = this.ui.toasts$;
  }

  ngOnInit(): void {
    this.updateActiveSectionFromUrl(this.router.url);
    this.navHighlight.category$
      .pipe(takeUntil(this.destroy$))
      .subscribe((category) => {
        this.navHighlightCategory = category;
        this.updateActiveSectionFromUrl(this.router.url);
      });
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((e) => this.updateActiveSectionFromUrl(e.urlAfterRedirects));

    this.session.user$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.currentUser = user ?? undefined;
      this.isAdmin = user?.roleName === 'ADMIN';
    });

    this.categoriesService.categories$
      .pipe(takeUntil(this.destroy$))
      .subscribe((cats) => {
        this.categories = cats;
        this.updateActiveSectionFromUrl(this.router.url);
      });
    this.categoriesService.refresh();

    this.pageService.pages$
      .pipe(takeUntil(this.destroy$))
      .subscribe((pages) => (this.footerPages = pages));
    this.pageService.load();
    this.socialLinksService.links$
      .pipe(takeUntil(this.destroy$))
      .subscribe((links) => (this.socialLinks = links));
    this.socialLinksService.load();
    this.siteSettingsService.settings$
      .pipe(takeUntil(this.destroy$))
      .subscribe((settings) => {
        if (settings?.title) {
          this.brandTitle = settings.title;
        } else {
          this.brandTitle = 'Toghrak X';
        }
        if (settings?.logoUrl) {
          this.brandLogo = settings.logoUrl;
        } else {
          this.brandLogo = 'assets/tn.png';
        }
        if (settings?.slogan) {
          this.brandTagline = settings.slogan;
        } else {
          this.brandTagline = 'Stories that matter, in one place.';
        }
      });
    this.siteSettingsService.load();
  }

  ngOnDestroy(): void {
    this.setBodyScrollLock(false);
    this.destroy$.next();
    this.destroy$.complete();
  }

  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/'); // go to home
    this.closeAllMenus();
  }

  dismissToast(id: number) {
    this.ui.dismiss(id);
  }

  goToCategory(cat?: Category) {
    if (!cat) {
      this.router.navigate(['/'], { queryParams: { page: 0 } });
    } else {
      this.router.navigate(['/category', cat.slug], { queryParams: { page: 0 } });
    }
    this.closeAllMenus();
  }

  onHeaderSearchSubmit(event: Event) {
    event.preventDefault();
    this.goToSearch(this.headerSearch);
  }

  goToSearch(query?: string) {
    const q = (query ?? '').trim();
    this.router.navigate(['/search'], { queryParams: { q: q || null } });
    this.closeAllMenus();
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
    if (!this.isMenuOpen) {
      this.isAccountMenuOpen = false;
    }
    this.setBodyScrollLock(this.isMenuOpen);
  }

  closeMenu() {
    this.closeAllMenus();
  }

  closeAllMenus() {
    this.isMenuOpen = false;
    this.isAccountMenuOpen = false;
    this.setBodyScrollLock(false);
  }

  onAccountMenuToggle(event: Event) {
    const details = event.target as HTMLDetailsElement | null;
    this.isAccountMenuOpen = !!details?.open;
  }

  // A <details> menu only closes from its own button. Like any dropdown, it should also close
  // when the user clicks anywhere outside it or presses Escape.
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (this.isAccountMenuOpen && !this.accountMenu?.nativeElement.contains(event.target as Node)) {
      this.isAccountMenuOpen = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey() {
    this.isAccountMenuOpen = false;
  }

  private updateActiveSectionFromUrl(url: string) {
    const path = (url.split('#')[0] ?? '').split('?')[0] ?? '';
    const clean = path.replace(/^\/+/, '').replace(/\/+$/, '');

    if (!clean) {
      this.isHomeActive = true;
      this.activeCategorySlug = null;
      return;
    }

    const parts = clean.split('/');
    const first = parts[0] ?? '';
    const second = parts[1] ?? '';
    this.isHomeActive = false;

    if (first === 'category') {
      this.activeCategorySlug = second || null;
    } else if (first === 'posts' && /^\d+$/.test(second)) {
      this.activeCategorySlug = this.resolveHighlightedCategorySlug();
    } else {
      this.activeCategorySlug = null;
    }
  }

  private resolveHighlightedCategorySlug(): string | null {
    const highlight = this.navHighlightCategory;
    if (!highlight) return null;
    if (!this.categories.length) return null;

    if (highlight.id != null) {
      const byId = this.categories.find((c) => c.id === highlight.id);
      return byId?.slug ?? null;
    }

    const name = (highlight.name ?? '').trim().toLowerCase();
    if (!name) return null;
    const byName = this.categories.find(
      (c) => c.name.trim().toLowerCase() === name
    );
    return byName?.slug ?? null;
  }

  private setBodyScrollLock(lock: boolean): void {
    if (typeof document === 'undefined') return;
    document.body.style.overflow = lock ? 'hidden' : '';
  }
}
