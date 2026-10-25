import { CategoriaResumen } from '../categoria/categoria.model';
export interface ProductoRequest {
  nombre: string;
  descripcion: string | null;
  precio: number;
  disponible: boolean;
  categoriaId: number;
  imagenUrl: string | null;
}
export interface Producto extends Omit<ProductoRequest, 'categoriaId'> {
  id: number;
  categoria: CategoriaResumen;
}
