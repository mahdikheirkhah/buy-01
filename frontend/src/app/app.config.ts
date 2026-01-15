import { ApplicationConfig, APP_INITIALIZER } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';
import { CookieService } from 'ngx-cookie-service';
import { AuthService } from './services/auth';
import { firstValueFrom } from 'rxjs';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])  // order: auth → error
    ),
    CookieService,
    {
      provide: APP_INITIALIZER,
      useFactory: (authService: AuthService) => () => {
        return firstValueFrom(authService.init()).catch(() => null);
      },
      deps: [AuthService],
      multi: true
    }
  ]
};
