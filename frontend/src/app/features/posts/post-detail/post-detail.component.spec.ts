import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, EMPTY, Subject, of } from 'rxjs';
import { PostDetailComponent } from './post-detail.component';
import { PostsService } from '../../../services/posts.service';
import { SessionService } from '../../../services/session.service';
import { NavHighlightService } from '../../../services/nav-highlight.service';
import { PostResponse } from '../../../models/post';

describe('PostDetailComponent', () => {
  let params: BehaviorSubject<ParamMap>;
  let responses: Record<string, Subject<PostResponse>>;

  const post = (slug: string, title: string): PostResponse => ({
    id: slug.length, slug, title, content: '<p>Body</p>', excerpt: '', contentType: 'html',
    categoryName: 'News', authorId: 1, authorUsername: 'editor', createdAt: '2026-10-23T10:00:00Z', imageUrls: [],
  });

  beforeEach(async () => {
    params = new BehaviorSubject(convertToParamMap({ slug: 'first' }));
    responses = { first: new Subject(), second: new Subject() };

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, PostDetailComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: ActivatedRoute, useValue: { paramMap: params.asObservable() } },
        {
          provide: PostsService,
          useValue: { getBySlug: (slug: string) => responses[slug], recordView: () => EMPTY },
        },
        { provide: SessionService, useValue: { user$: of(null), isAdmin$: of(false) } },
        { provide: NavHighlightService, useValue: { setCategory: () => {}, clear: () => {} } },
      ],
    }).compileComponents();
  });

  it('loads the new post when the slug in the address changes', () => {
    const component = TestBed.createComponent(PostDetailComponent).componentInstance;
    component.ngOnInit();

    responses['first'].next(post('first', 'First post'));
    expect(component.post?.title).toBe('First post');

    params.next(convertToParamMap({ slug: 'second' }));
    responses['second'].next(post('second', 'Second post'));
    expect(component.post?.title).toBe('Second post');
  });

  it('ignores a slow answer for a post the reader has already left', () => {
    const component = TestBed.createComponent(PostDetailComponent).componentInstance;
    component.ngOnInit();

    params.next(convertToParamMap({ slug: 'second' })); // reader moves on before "first" answered
    responses['second'].next(post('second', 'Second post'));
    responses['first'].next(post('first', 'First post')); // the late answer arrives

    expect(component.post?.title).toBe('Second post');
  });
});
