import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withRouterConfig } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { QuillModule } from 'ngx-quill';
import { routes } from './app.routes';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { HttpErrorInterceptor } from './interceptors/http-error.interceptor';
import { EDITOR_CONFIG } from './editor-config';

export const appConfig: ApplicationConfig = {
  providers: [
    // Angular 21 runs without zone.js unless this is provided. Components here still update their
    // fields inside subscribe() callbacks and rely on zone.js to refresh the screen.
    provideZoneChangeDetection(),
    provideRouter(routes, withRouterConfig({ onSameUrlNavigation: 'reload' })),
    // Class-based interceptors, registered through DI; they become functions in the next step.
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true },
    importProvidersFrom(QuillModule.forRoot(EDITOR_CONFIG)),
  ],
};
