import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap } from '@angular/router';
import { BehaviorSubject, Subject } from 'rxjs';
import { PageViewComponent } from './page-view.component';
import { PagesService } from '../../../services/pages.service';
import { ContentPage } from '../../../models/content-page';

describe('PageViewComponent', () => {
  let params: BehaviorSubject<ParamMap>;
  let responses: Record<string, Subject<ContentPage>>;

  beforeEach(async () => {
    params = new BehaviorSubject(convertToParamMap({ slug: 'about' }));
    responses = { about: new Subject(), faq: new Subject() };

    await TestBed.configureTestingModule({
      declarations: [PageViewComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: ActivatedRoute, useValue: { paramMap: params.asObservable() } },
        { provide: PagesService, useValue: { get: (slug: string) => responses[slug] } },
      ],
    }).compileComponents();
  });

  it('ignores a slow answer for a page the reader has already left', () => {
    const component = TestBed.createComponent(PageViewComponent).componentInstance;
    component.ngOnInit();

    params.next(convertToParamMap({ slug: 'faq' })); // reader moves on before "about" answered
    responses['faq'].next({ slug: 'faq', title: 'FAQ', content: '<p>Q&A</p>' } as ContentPage);
    responses['about'].next({ slug: 'about', title: 'About', content: '<p>Us</p>' } as ContentPage);

    expect(component.page?.title).toBe('FAQ');
  });
});
