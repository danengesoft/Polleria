import { Injectable, computed, signal } from '@angular/core';
import { Producto } from '../catalogo/producto/producto.model';
export interface LineaCarrito {
  productoId: number;
  nombre: string;
  precio: number;
  cantidad: number;
}
export const CARRITO_KEY = 'polleria_carrito';
@Injectable({ providedIn: 'root' })
export class CarritoService {
  private readonly estado = signal<LineaCarrito[]>(this.restaurar());
  readonly lineas = this.estado.asReadonly();
  readonly unidades = computed(() => this.lineas().reduce((n, l) => n + l.cantidad, 0));
  readonly total = computed(
    () => this.lineas().reduce((n, l) => n + Math.round(l.precio * 100) * l.cantidad, 0) / 100,
  );
  agregar(p: Producto) {
    if (!p.disponible) return;
    const actual = this.lineas().find((l) => l.productoId === p.id);
    if (actual) this.cantidad(p.id, actual.cantidad + 1);
    else
      this.guardar([
        ...this.lineas(),
        { productoId: p.id, nombre: p.nombre, precio: p.precio, cantidad: 1 },
      ]);
  }
  cantidad(id: number, cantidad: number) {
    if (!Number.isSafeInteger(cantidad) || cantidad < 1 || cantidad > 999) return;
    this.guardar(this.lineas().map((l) => (l.productoId === id ? { ...l, cantidad } : l)));
  }
  eliminar(id: number) {
    this.guardar(this.lineas().filter((l) => l.productoId !== id));
  }
  vaciar() {
    this.guardar([]);
  }
  private guardar(lineas: LineaCarrito[]) {
    this.estado.set(lineas);
    try {
      sessionStorage.setItem(CARRITO_KEY, JSON.stringify(lineas));
    } catch {
      /* Memory remains usable when storage is unavailable. */
    }
  }
  private restaurar(): LineaCarrito[] {
    try {
      const value: unknown = JSON.parse(sessionStorage.getItem(CARRITO_KEY) || '[]');
      if (!Array.isArray(value) || value.length > 100) return [];
      const ids = new Set<number>();
      return value.filter((l): l is LineaCarrito => {
        if (
          !l ||
          !Number.isSafeInteger(l.productoId) ||
          l.productoId < 1 ||
          typeof l.nombre !== 'string' ||
          l.nombre.length > 200 ||
          !Number.isFinite(l.precio) ||
          l.precio <= 0 ||
          !Number.isSafeInteger(l.cantidad) ||
          l.cantidad < 1 ||
          l.cantidad > 999 ||
          ids.has(l.productoId)
        )
          return false;
        ids.add(l.productoId);
        return true;
      });
    } catch {
      return [];
    }
  }
}
