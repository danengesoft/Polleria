import { signal } from '@angular/core';
import { Rol } from '../core/auth/auth-service';
// Solo fixtures de pruebas: nunca se importa desde el código de producción.
export function authFixture(initial: Rol[] = ['ADMINISTRADOR']) {
  const roles = signal(initial);
  return {
    roles,
    error: signal(''),
    autenticado: () => true,
    usuario: () => ({ sub: 'test-user', nombre: 'Usuario de prueba' }),
    tieneRol: (...permitted: Rol[]) => permitted.some((role) => roles().includes(role)),
    asegurarSesion: async () => true,
    iniciarLogin: async () => {},
    accessToken: (): string | null => null,
    refrescar: async () => false,
    descartarSesion: () => {},
    cerrarSesion: () => {},
  };
}
