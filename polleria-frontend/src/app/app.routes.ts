import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'interno',
    loadComponent: () => import('./core/layout/interno/interno').then((m) => m.Interno),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Portal interno | Pollería',
        loadComponent: () => import('./core/inicio/inicio-interno').then((m) => m.InicioInterno),
      },
      {
        path: 'categorias',
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-list').then((m) => m.CategoriaList),
      },
      {
        path: 'categorias/nuevo',
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-form').then((m) => m.CategoriaForm),
      },
      {
        path: 'categorias/:id/editar',
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-form').then((m) => m.CategoriaForm),
      },
      {
        path: 'productos',
        loadComponent: () =>
          import('./features/catalogo/producto/producto-list').then((m) => m.ProductoList),
      },
      {
        path: 'productos/nuevo',
        loadComponent: () =>
          import('./features/catalogo/producto/producto-form').then((m) => m.ProductoForm),
      },
      {
        path: 'productos/:id/editar',
        loadComponent: () =>
          import('./features/catalogo/producto/producto-form').then((m) => m.ProductoForm),
      },
      {
        path: 'clientes',
        loadComponent: () =>
          import('./features/clientes/cliente/cliente-list').then((m) => m.ClienteList),
      },
      {
        path: 'clientes/nuevo',
        loadComponent: () =>
          import('./features/clientes/cliente/cliente-form').then((m) => m.ClienteForm),
      },
      {
        path: 'clientes/:id/editar',
        loadComponent: () =>
          import('./features/clientes/cliente/cliente-form').then((m) => m.ClienteForm),
      },
      {
        path: 'pedidos',
        title: 'Pedidos | Pollería',
        loadComponent: () => import('./features/pedidos/pedido-list').then((m) => m.PedidoList),
      },
      {
        path: 'pedidos/nuevo',
        title: 'Nuevo pedido | Pollería',
        loadComponent: () => import('./features/pedidos/pedido-form').then((m) => m.PedidoForm),
      },
      {
        path: 'pedidos/:id',
        title: 'Detalle del pedido | Pollería',
        loadComponent: () =>
          import('./features/pedidos/pedido-detalle').then((m) => m.PedidoDetalle),
      },
      {
        path: 'conexion',
        title: 'Conexión | Pollería',
        loadComponent: () => import('./core/conexion/conexion').then((m) => m.Conexion),
      },
    ],
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/publico/publico').then((m) => m.Publico),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Inicio | Pollería',
        loadComponent: () => import('./core/inicio/inicio-publico').then((m) => m.InicioPublico),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
