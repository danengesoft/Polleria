import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductoService } from '../catalogo/producto/producto-service';
import { CategoriaService } from '../catalogo/categoria/categoria-service';
import { Producto } from '../catalogo/producto/producto.model';
import { Categoria } from '../catalogo/categoria/categoria.model';
import { CarritoService } from './carrito-service';
import { mensajeHttp } from '../../shared/http-error';
@Component({
  selector: 'app-catalogo-publico',
  imports: [CurrencyPipe, RouterLink],
  styleUrl: './publico.css',
  template: ` <p class="eyebrow">A tu gusto</p>
    <h1>Nuestro catálogo</h1>
    <p>Elige tus productos y prepara tu pedido. Los precios se confirman al ordenar.</p>
    <div class="catalog-filter">
      <label for="categoria">Categoría</label
      ><select id="categoria" (change)="cargar($any($event.target).value)">
        <option value="">Todos</option>
        @for (c of categorias(); track c.id) {
          <option [value]="c.id">{{ c.nombre }}</option>
        }
      </select>
    </div>
    @if (error()) {
      <p role="alert">{{ error() }}</p>
      <button (click)="cargar()">Reintentar</button>
    }
    @if (cargando()) {
      <p role="status">Cargando catálogo…</p>
    }
    <p role="status">{{ aviso() }}</p>
    <div class="cards">
      @for (p of productos(); track p.id) {
        <article class="card" [class.unavailable]="!p.disponible">
          @if (imagen(p.imagenUrl) && !imagenesFallidas().has(p.id)) {
            <img
              [src]="imagen(p.imagenUrl)"
              [alt]="p.nombre"
              loading="lazy"
              referrerpolicy="no-referrer"
              (error)="imagenFallida(p.id)"
            />
          } @else {
            <div class="placeholder" aria-hidden="true">Sin imagen</div>
          }
          <div class="product-info">
            <p class="eyebrow">{{ p.categoria.nombre }}</p>
            <h2>{{ p.nombre }}</h2>
            <p>{{ p.descripcion }}</p>
            <strong class="price">{{ p.precio | currency: 'PEN' : 'S/ ' : '1.2-2' }}</strong>
            @if (p.disponible) {
              <button (click)="agregar(p)">Agregar al carrito</button>
            } @else {
              <p class="availability">No disponible</p>
              <button class="secondary" disabled>Agregar al carrito</button>
            }
          </div>
        </article>
      } @empty {
        @if (!cargando() && !error()) {
          <p>No hay productos en esta categoría.</p>
        }
      }
    </div>
    <a routerLink="/carrito">Ver carrito</a>`,
})
export class CatalogoPublico {
  private readonly productosApi = inject(ProductoService);
  private readonly categoriasApi = inject(CategoriaService);
  readonly carrito = inject(CarritoService);
  readonly imagenesFallidas = signal<Set<number>>(new Set());
  imagenFallida(id: number) {
    this.imagenesFallidas.update((ids) => new Set([...ids, id]));
  }
  readonly productos = signal<Producto[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly cargando = signal(false);
  constructor() {
    this.categoriasApi.listar().subscribe({
      next: (c) => this.categorias.set(c),
      error: (e) => this.error.set(mensajeHttp(e)),
    });
    this.cargar();
  }
  cargar(id = '') {
    this.cargando.set(true);
    this.error.set('');
    this.productosApi.listar(id ? Number(id) : undefined).subscribe({
      next: (p) => {
        this.productos.set(p);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeHttp(e));
        this.cargando.set(false);
      },
    });
  }
  agregar(p: Producto) {
    this.carrito.agregar(p);
    this.aviso.set(p.nombre + ' agregado al carrito.');
  }
  imagen(url: string | null) {
    if (!url) return null;
    try {
      const u = new URL(url);
      return ['http:', 'https:'].includes(u.protocol) ? u.href : null;
    } catch {
      return null;
    }
  }
}
