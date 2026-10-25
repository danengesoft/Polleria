export interface CategoriaRequest {
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}
export interface Categoria extends CategoriaRequest {
  id: number;
}
export interface CategoriaResumen {
  id: number;
  nombre: string;
}
