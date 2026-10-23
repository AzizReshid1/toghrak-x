import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['getToken', 'logout', 'refresh']);
    auth.getToken.and.returnValue('token');
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sends the token only to our own API, never to other sites', () => {
    http.get('/api/posts').subscribe();
    expect(httpMock.expectOne('/api/posts').request.headers.get('Authorization')).toBe('Bearer token');

    http.get('https://other-site.example/data').subscribe();
    expect(httpMock.expectOne('https://other-site.example/data').request.headers.has('Authorization')).toBeFalse();

    http.get('/apiary').subscribe(); // only the /api prefix itself, not URLs that merely start alike
    expect(httpMock.expectOne('/apiary').request.headers.has('Authorization')).toBeFalse();
  });

  it('keeps the session on 403 (not allowed is not logged out)', () => {
    http.put('/api/posts/1', {}).subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/posts/1').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(auth.logout).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('logs out when a 401 cannot be fixed by refreshing the token', () => {
    auth.refresh.and.returnValue(throwError(() => new Error('refresh failed')));

    http.get('/api/users/me').subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/users/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('retries a request that got 401 with the token from the refresh', () => {
    auth.refresh.and.returnValue(of({ token: 'new-token' }));

    http.get('/api/users/me').subscribe();
    httpMock.expectOne('/api/users/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    const retry = httpMock.expectOne('/api/users/me');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new-token');
    retry.flush({});
  });

  it('logs out only once when several requests fail and the refresh fails', () => {
    let token: string | null = 'expired-token';
    auth.getToken.and.callFake(() => token);
    auth.logout.and.callFake(() => (token = null));
    auth.refresh.and.returnValue(throwError(() => new Error('refresh failed')));

    http.get('/api/a').subscribe({ next: () => {}, error: () => {} });
    http.get('/api/b').subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/a').flush(null, { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/b').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
