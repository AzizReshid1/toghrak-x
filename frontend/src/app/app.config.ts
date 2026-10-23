import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withRouterConfig } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { QuillModule } from 'ngx-quill';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';
import { httpErrorInterceptor } from './interceptors/http-error.interceptor';
import { EDITOR_CONFIG } from './editor-config';

export const appConfig: ApplicationConfig = {
  providers: [
    // Angular 21 runs without zone.js unless this is provided. Components here still update their
    // fields inside subscribe() callbacks and rely on zone.js to refresh the screen.
    provideZoneChangeDetection(),
    provideRouter(routes, withRouterConfig({ onSameUrlNavigation: 'reload' })),
    // Order matters: authInterceptor adds the token and refreshes it on 401 before
    // httpErrorInterceptor decides which failures to show as a toast.
    provideHttpClient(withInterceptors([authInterceptor, httpErrorInterceptor])),
    importProvidersFrom(QuillModule.forRoot(EDITOR_CONFIG)),
  ],
};
