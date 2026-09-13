import { TestBed } from '@angular/core/testing';
import { convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { catalogoResolver } from './catalogo.resolver';
import { libroDetalleResolver } from './libro-detalle.resolver';
import { LibroService } from '../services/libro.service';
import { CategoriaService } from '../services/categoria.service';
import { ToastService } from '../../shared/toast/toast.service';

// Regresión loading-infinito: los resolvers nunca deben retornar EMPTY
// (la navegación se cancela en silencio, sin NavigationEnd, y el shell
// queda en cargando para siempre). Ante error emiten un valor vacío y
// la ruta activa mostrando su estado de error/vacío.
describe('resolvers anti loading-infinito', () => {
  let libroService: jasmine.SpyObj<LibroService>;
  let categoriaService: jasmine.SpyObj<CategoriaService>;

  beforeEach(() => {
    libroService = jasmine.createSpyObj('LibroService', ['listar', 'obtener']);
    categoriaService = jasmine.createSpyObj('CategoriaService', ['listar']);

    TestBed.configureTestingModule({
      providers: [
        { provide: LibroService, useValue: libroService },
        { provide: CategoriaService, useValue: categoriaService },
        { provide: ToastService, useValue: { warning: () => undefined, error: () => undefined } },
      ]
    });
  });

  it('catalogoResolver emite datos vacíos si el backend falla (no cancela)', (done) => {
    libroService.listar.and.returnValue(throwError(() => ({ status: 500 })));
    categoriaService.listar.and.returnValue(of([]));

    TestBed.runInInjectionContext(() => {
      catalogoResolver({} as any, {} as any).subscribe({
        next: (data: any) => {
          expect(data.libros.content).toEqual([]);
          expect(data.categorias).toEqual([]);
          done();
        },
        error: () => fail('no debe emitir error')
      });
    });
  });

  it('catalogoResolver deja pasar los datos cuando el backend responde', (done) => {
    const pagina = { content: [{ id: 1 }], totalPages: 1, totalElements: 1 };
    libroService.listar.and.returnValue(of(pagina));
    categoriaService.listar.and.returnValue(of([{ id: 2 }]));

    TestBed.runInInjectionContext(() => {
      catalogoResolver({} as any, {} as any).subscribe((data: any) => {
        expect(data.libros).toBe(pagina);
        expect(data.categorias).toEqual([{ id: 2 }]);
        done();
      });
    });
  });

  it('libroDetalleResolver emite null si el backend falla (no cancela)', (done) => {
    libroService.obtener.and.returnValue(throwError(() => ({ status: 500 })));
    const route = { paramMap: convertToParamMap({ id: '7' }) } as any;

    TestBed.runInInjectionContext(() => {
      libroDetalleResolver(route, {} as any).subscribe({
        next: (data) => {
          expect(data).toBeNull();
          done();
        },
        error: () => fail('no debe emitir error')
      });
    });
  });

  it('libroDetalleResolver emite null sin llamar al backend si el id es inválido', () => {
    const route = { paramMap: convertToParamMap({}) } as any;

    TestBed.runInInjectionContext(() => {
      libroDetalleResolver(route, {} as any).subscribe((data) => {
        expect(data).toBeNull();
      });
    });

    expect(libroService.obtener).not.toHaveBeenCalled();
  });
});
