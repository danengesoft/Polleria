import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService, INTERNOS } from './auth-service';
@Component({
  selector: 'app-auth-callback',
  imports: [RouterLink],
  template: `<main class="container auth-page">
    @if (error()) {
      <h1>No se pudo iniciar sesión</h1>
      <p role="alert">{{ error() }}</p>
      <a routerLink="/">Volver al inicio</a>
    } @else {
      <p role="status">Completando inicio de sesión…</p>
    }
  </main>`,
  styles: '.auth-page { padding-block:3rem; }',
})
export class AuthCallback {
  readonly error = signal('');
  constructor() {
    const route = inject(ActivatedRoute);
    const router = inject(Router);
    const auth = inject(AuthService);
    const code = route.snapshot.queryParamMap.get('code');
    const state = route.snapshot.queryParamMap.get('state');
    history.replaceState(history.state, '', '/auth/callback');
    if (route.snapshot.queryParamMap.has('error') || !code || !state) {
      auth.limpiarFlujo();
      this.error.set(
        'No se pudo completar el inicio de sesión. Vuelve a intentarlo desde el inicio.',
      );
      return;
    }
    auth
      .completarLogin(code, state)
      .then(() => {
        let destino = auth.destinoPendiente();
        if (destino.startsWith('/interno') && !auth.tieneRol(...INTERNOS)) destino = '/sin-permiso';
        if (destino === '/' && auth.tieneRol(...INTERNOS)) destino = '/interno';
        return router.navigateByUrl(destino, { replaceUrl: true });
      })
      .catch(() =>
        this.error.set(
          'No se pudo validar el inicio de sesión. Vuelve a intentarlo desde el inicio.',
        ),
      );
  }
}
