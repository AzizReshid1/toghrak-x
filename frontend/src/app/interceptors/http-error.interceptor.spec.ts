import { HTTP_INTERCEPTORS, HttpClient, HttpContext } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorInterceptor, SKIP_ERROR_TOAST } from './http-error.interceptor';
import { UiMessageService } from '../services/ui-message.service';

describe('HttpErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let ui: UiMessageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        UiMessageService,
        { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    ui = TestBed.inject(UiMessageService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('shows a network error message when status is 0', () => {
    spyOn(ui, 'show').and.callThrough();

    http.get('/api/ping').subscribe({ next: () => {}, error: () => {} });
    const req = httpMock.expectOne('/api/ping');
    req.error(new ProgressEvent('error'));

    expect(ui.show).toHaveBeenCalledWith({
      kind: 'error',
      text: 'Cannot reach the server. Check your connection and that the backend is running.',
    });
  });

  it('shows a server error message when status is 500+', () => {
    spyOn(ui, 'show').and.callThrough();

    http.get('/api/ping').subscribe({ next: () => {}, error: () => {} });
    const req = httpMock.expectOne('/api/ping');
    req.flush({ message: 'boom' }, { status: 500, statusText: 'Server error' });

    expect(ui.show).toHaveBeenCalledWith({
      kind: 'error',
      text: 'Server error. Please try again.',
    });
  });

  it('does not show a message for 4xx errors', () => {
    spyOn(ui, 'show').and.callThrough();

    http.get('/api/ping').subscribe({ next: () => {}, error: () => {} });
    const req = httpMock.expectOne('/api/ping');
    req.flush({ message: 'bad request' }, { status: 400, statusText: 'Bad request' });

    expect(ui.show).not.toHaveBeenCalled();
  });

  it('shows the ProblemDetail reason from the server when a save fails', () => {
    spyOn(ui, 'show').and.callThrough();

    http.delete('/api/categories/1').subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/categories/1').flush(
      { status: 409, detail: 'Cannot delete category: there are still 3 post(s) assigned to it.' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(ui.show).toHaveBeenCalledWith({
      kind: 'error',
      text: 'Cannot delete category: there are still 3 post(s) assigned to it.',
    });
  });

  it('shows the field messages of a validation error', () => {
    spyOn(ui, 'show').and.callThrough();

    http.post('/api/users', {}).subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/users').flush(
      { status: 400, detail: 'Validation failed.', errors: { password: 'Password must be between 8 and 72 characters' } },
      { status: 400, statusText: 'Bad request' }
    );

    expect(ui.show).toHaveBeenCalledWith({ kind: 'error', text: 'Password must be between 8 and 72 characters' });
  });

  it('falls back to a plain message when the response has no reason', () => {
    spyOn(ui, 'show').and.callThrough();

    http.put('/api/pages/about', {}).subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/pages/about').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(ui.show).toHaveBeenCalledWith({ kind: 'error', text: 'You do not have permission to do that.' });
  });

  it('leaves 401 to the auth flow, except for a failed login', () => {
    spyOn(ui, 'show').and.callThrough();

    http.post('/api/auth/refresh', {}).subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(ui.show).not.toHaveBeenCalled();

    http.post('/api/auth/login', {}).subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(ui.show).toHaveBeenCalledWith({ kind: 'error', text: 'Wrong username or password.' });
  });

  it('stays silent for requests marked SKIP_ERROR_TOAST', () => {
    spyOn(ui, 'show').and.callThrough();

    http.post('/api/posts/1/view', null, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .subscribe({ next: () => {}, error: () => {} });
    httpMock.expectOne('/api/posts/1/view').flush(null, { status: 404, statusText: 'Not found' });

    expect(ui.show).not.toHaveBeenCalled();
  });
});
