import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';
import { AuthInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('AuthInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['getToken', 'logout', 'refresh']);
    auth.getToken.and.returnValue('token');
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
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
});
