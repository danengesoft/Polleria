import { inject } from '@angular/core';
import { CanActivateFn, CanActivateChildFn } from '@angular/router';
import { AuthService } from '../auth/auth-service';
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  if (await auth.asegurarSesion()) return true;
  await auth.iniciarLogin(state.url);
  return false;
};
export const authChildGuard: CanActivateChildFn = authGuard;
