import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sends one refresh request for any number of simultaneous callers', () => {
    const tokens: string[] = [];
    service.refresh().subscribe((res) => tokens.push(res.token));
    service.refresh().subscribe((res) => tokens.push(res.token));

    httpMock.expectOne('/api/auth/refresh').flush({ token: 'fresh' });

    expect(tokens).toEqual(['fresh', 'fresh']);
    expect(service.getToken()).toBe('fresh');
  });

  it('allows a new refresh once the previous one has finished', () => {
    service.refresh().subscribe();
    httpMock.expectOne('/api/auth/refresh').flush({ token: 'first' });

    service.refresh().subscribe();
    httpMock.expectOne('/api/auth/refresh').flush({ token: 'second' });

    expect(service.getToken()).toBe('second');
  });
});
