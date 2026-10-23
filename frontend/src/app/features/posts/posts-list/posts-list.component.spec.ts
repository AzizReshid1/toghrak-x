import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PostsListComponent } from './posts-list.component';
import { PostsService } from '../../../services/posts.service';
import { CategoriesService } from '../../../services/categories.service';
import { SessionService } from '../../../services/session.service';

describe('PostsListComponent', () => {
  let router: jasmine.SpyObj<Router>;

  function openCategory(categoryAnswer: any): PostsListComponent {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      imports: [PostsListComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ slug: 'no-such-category' })), queryParamMap: of(convertToParamMap({})) },
        },
        { provide: CategoriesService, useValue: { getBySlug: () => categoryAnswer } },
        { provide: PostsService, useValue: { list: () => of({ content: [], page: {} }), popular: () => of([]) } },
        { provide: SessionService, useValue: { user$: of(null), isAdmin$: of(false), isLoggedIn$: of(false) } },
      ],
    });
    const component = TestBed.createComponent(PostsListComponent).componentInstance;
    component.ngOnInit();
    return component;
  }

  it('shows the 404 page for an unknown category and keeps the address', () => {
    openCategory(throwError(() => new HttpErrorResponse({ status: 404 })));

    expect(router.navigate).toHaveBeenCalledWith(['/not-found'], { skipLocationChange: true });
  });

  it('shows an error, not the 404 page, when the server fails', () => {
    const component = openCategory(throwError(() => new HttpErrorResponse({ status: 500 })));

    expect(router.navigate).not.toHaveBeenCalled();
    expect(component.error).toBe('Failed to load posts.');
  });
});
