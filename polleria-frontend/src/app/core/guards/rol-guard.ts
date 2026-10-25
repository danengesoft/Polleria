import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Rol } from '../auth/auth-service';
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return () => inject(AuthService).tieneRol(...roles) || inject(Router).parseUrl('/sin-permiso');
}
