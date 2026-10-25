import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth-service';
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Exact API origin and path boundary, never Keycloak or a third party.
  if (!req.url.startsWith(environment.apiBaseUrl + '/api/')) return next(req);
  const auth = inject(AuthService);
  const router = inject(Router);
  const tokenInicial = auth.accessToken();
  const bearer = () =>
    auth.accessToken()
      ? req.clone({ setHeaders: { Authorization: `Bearer ${auth.accessToken()}` } })
      : req;
  const fallo = (error: HttpErrorResponse) => {
    if (error.status === 403) void router.navigateByUrl('/sin-permiso');
    return throwError(() => error);
  };
  const reautenticar = (error: HttpErrorResponse) => {
    auth.descartarSesion();
    void auth.iniciarLogin(router.url);
    return throwError(() => error);
  };
  return next(bearer()).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) return fallo(error);
      return from(
        auth.accessToken() && auth.accessToken() !== tokenInicial
          ? Promise.resolve(true)
          : auth.refrescar(),
      ).pipe(
        switchMap((ok) => {
          if (!ok) return reautenticar(error);
          return next(bearer()).pipe(
            catchError((retry: HttpErrorResponse) =>
              retry.status === 401 ? reautenticar(retry) : fallo(retry),
            ),
          );
        }),
      );
    }),
  );
};
