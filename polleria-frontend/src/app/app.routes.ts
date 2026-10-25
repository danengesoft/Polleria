import { authGuard, authChildGuard } from './core/guards/auth-guard';
import { rolGuard } from './core/guards/rol-guard';
import { INTERNOS, OPERATIVOS } from './core/auth/auth-service';
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'auth/callback',
    loadComponent: () => import('./core/auth/auth-callback').then((m) => m.AuthCallback),
  },
  {
    path: 'sin-permiso',
    loadComponent: () => import('./core/auth/sin-permiso').then((m) => m.SinPermiso),
  },
  {
    path: 'interno',
    canActivate: [authGuard, rolGuard(...INTERNOS)],
    canActivateChild: [authChildGuard, rolGuard(...INTERNOS)],
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
        canActivate: [rolGuard(...OPERATIVOS)],
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-list').then((m) => m.CategoriaList),
      },
      {
        path: 'categorias/nuevo',
        canActivate: [rolGuard('ADMINISTRADOR')],
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-form').then((m) => m.CategoriaForm),
      },
      {
        path: 'categorias/:id/editar',
        canActivate: [rolGuard('ADMINISTRADOR')],
        loadComponent: () =>
          import('./features/catalogo/categoria/categoria-form').then((m) => m.CategoriaForm),
      },
      {
        path: 'productos',
        canActivate: [rolGuard(...OPERATIVOS)],
        loadComponent: () =>
          import('./features/catalogo/producto/producto-list').then((m) => m.ProductoList),
      },
      {
        path: 'productos/nuevo',
        canActivate: [rolGuard('ADMINISTRADOR')],
        loadComponent: () =>
          import('./features/catalogo/producto/producto-form').then((m) => m.ProductoForm),
      },
      {
        path: 'productos/:id/editar',
        canActivate: [rolGuard('ADMINISTRADOR')],
        loadComponent: () =>
          import('./features/catalogo/producto/producto-form').then((m) => m.ProductoForm),
      },
      {
        path: 'clientes',
        canActivate: [rolGuard(...OPERATIVOS)],
        loadComponent: () =>
          import('./features/clientes/cliente/cliente-list').then((m) => m.ClienteList),
      },
      {
        path: 'clientes/nuevo',
        canActivate: [rolGuard(...OPERATIVOS)],
        loadComponent: () =>
          import('./features/clientes/cliente/cliente-form').then((m) => m.ClienteForm),
      },
      {
        path: 'clientes/:id/editar',
        canActivate: [rolGuard(...OPERATIVOS)],
        loadComponent: () =>
          import('./features/clientes/cliente/cliente-form').then((m) => m.ClienteForm),
      },
      {
        path: 'pedidos',
        canActivate: [rolGuard(...OPERATIVOS)],
        title: 'Pedidos | Pollería',
        loadComponent: () => import('./features/pedidos/pedido-list').then((m) => m.PedidoList),
      },
      {
        path: 'pedidos/nuevo',
        canActivate: [rolGuard(...OPERATIVOS)],
        title: 'Nuevo pedido | Pollería',
        loadComponent: () => import('./features/pedidos/pedido-form').then((m) => m.PedidoForm),
      },
      {
        path: 'pedidos/:id',
        canActivate: [rolGuard(...OPERATIVOS)],
        title: 'Detalle del pedido | Pollería',
        loadComponent: () =>
          import('./features/pedidos/pedido-detalle').then((m) => m.PedidoDetalle),
      },
      {
        path: 'delivery',
        canActivate: [rolGuard(...OPERATIVOS)],
        loadComponent: () =>
          import('./features/delivery/delivery-list').then((m) => m.DeliveryList),
      },
      {
        path: 'reparto',
        canActivate: [rolGuard('REPARTIDOR')],
        loadComponent: () => import('./features/delivery/mis-entregas').then((m) => m.MisEntregas),
      },
      {
        path: 'abastecimiento/insumos',
        canActivate: [rolGuard('ADMINISTRADOR', 'ABASTECIMIENTO')],
        loadComponent: () => import('./features/abastecimiento/insumos').then((m) => m.Insumos),
      },
      {
        path: 'abastecimiento/proveedores',
        canActivate: [rolGuard('ADMINISTRADOR', 'ABASTECIMIENTO')],
        loadComponent: () =>
          import('./features/abastecimiento/proveedores').then((m) => m.Proveedores),
      },
      {
        path: 'abastecimiento/compras',
        canActivate: [rolGuard('ADMINISTRADOR', 'ABASTECIMIENTO')],
        loadComponent: () => import('./features/abastecimiento/compras').then((m) => m.Compras),
      },
      {
        path: 'abastecimiento/inventario',
        canActivate: [rolGuard('ADMINISTRADOR', 'ABASTECIMIENTO')],
        loadComponent: () =>
          import('./features/abastecimiento/inventario').then((m) => m.Inventario),
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
        path: 'catalogo',
        loadComponent: () =>
          import('./features/publico/catalogo-publico').then((m) => m.CatalogoPublico),
      },
      {
        path: 'carrito',
        loadComponent: () => import('./features/publico/carrito').then((m) => m.Carrito),
      },
      {
        path: 'perfil',
        canActivate: [authGuard, rolGuard('CLIENTE')],
        loadComponent: () => import('./features/publico/perfil').then((m) => m.Perfil),
      },
      {
        path: 'checkout',
        canActivate: [authGuard, rolGuard('CLIENTE')],
        loadComponent: () => import('./features/publico/checkout').then((m) => m.Checkout),
      },
      {
        path: 'mis-pedidos',
        canActivate: [authGuard, rolGuard('CLIENTE')],
        loadComponent: () => import('./features/publico/mis-pedidos').then((m) => m.MisPedidos),
      },
      {
        path: 'mis-pedidos/:id',
        canActivate: [authGuard, rolGuard('CLIENTE')],
        loadComponent: () => import('./features/publico/mi-pedido').then((m) => m.MiPedido),
      },
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
