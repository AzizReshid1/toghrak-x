import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { UiMessageService } from '../services/ui-message.service';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  constructor(private ui: UiMessageService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 0) {
          this.ui.show({
            kind: 'error',
            text: 'Cannot reach the server. Check your connection and that the backend is running.',
          });
        } else if (err.status === 404) {
          this.ui.show({ kind: 'error', text: 'The requested resource was not found.' });
        } else if (err.status >= 500) {
          this.ui.show({
            kind: 'error',
            text: 'Server error. Please try again.',
          });
        }
        return throwError(() => err);
      })
    );
  }
}

