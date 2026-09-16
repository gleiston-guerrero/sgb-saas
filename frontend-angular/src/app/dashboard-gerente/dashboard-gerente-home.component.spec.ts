import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { DashboardGerenteHomeComponent } from './dashboard-gerente-home.component';
import { ReporteService, LibroMasPrestado, ReporteMorosidad } from '../core/services/reporte-gerencial.service';
import { AuthService } from '../core/services/auth.service';
import { MultaService } from '../core/services/multa.service';
import { ResumenFinancieroMultas } from '../core/models/multa.model';
import { AuditoriaService } from '../core/services/auditoria.service';
import { Page } from '../core/models/pagina.model';
import { EventoAuditoria } from '../core/models/evento-auditoria.model';

describe('DashboardGerenteHomeComponent', () => {
  let component: DashboardGerenteHomeComponent;
  let fixture: ComponentFixture<DashboardGerenteHomeComponent>;
  let reporteService: jasmine.SpyObj<ReporteService>;
  let multaService: jasmine.SpyObj<MultaService>;
  let auditoriaService: jasmine.SpyObj<AuditoriaService>;

  const seisLibros: LibroMasPrestado[] = [1, 2, 3, 4, 5, 6].map(n => ({
    libroId: n,
    titulo: `Libro ${n}`,
    isbn: `ISBN-${n}`,
    totalPrestamos: 40 - n
  }));

  const resumenFinanciero: ResumenFinancieroMultas = {
    totalRecaudado: 125.5,
    totalPendiente: 40,
    totalGeneradoHoy: 10,
    pagosRecientes: []
  };

  const dosMorosos: ReporteMorosidad[] = [
    { usuarioId: 1, nombre: 'Ana', apellido: 'Pérez', correo: 'ana@correo.com', montoTotalAdeudado: 15.5, cantidadMultasPendientes: 1, diasAtrasoPromedio: 3 },
    { usuarioId: 2, nombre: 'Luis', apellido: 'Gómez', correo: 'luis@correo.com', montoTotalAdeudado: 24.5, cantidadMultasPendientes: 2, diasAtrasoPromedio: 5 }
  ];

  const paginaAuditoria: Page<EventoAuditoria> = {
    content: [
      { id: 1, usuario: 'admin@correo.com', accion: 'UPDATE', fechaHora: '2026-08-19T10:00:00Z', modulo: 'usuarios', detalle: 'ejemplo' }
    ],
    totalPages: 1,
    totalElements: 1,
    size: 5,
    number: 0,
    numberOfElements: 1,
    empty: false
  };

  beforeEach(async () => {
    reporteService = jasmine.createSpyObj('ReporteService', ['librosMasPrestados', 'morosidad']);
    multaService = jasmine.createSpyObj('MultaService', ['resumenFinanciero']);
    auditoriaService = jasmine.createSpyObj('AuditoriaService', ['listar']);

    // Valores por defecto (happy path): los tests de error los sobrescriben.
    reporteService.librosMasPrestados.and.returnValue(of(seisLibros));
    reporteService.morosidad.and.returnValue(of({ content: dosMorosos, totalPages: 1, totalElements: 2 }));
    multaService.resumenFinanciero.and.returnValue(of(resumenFinanciero));
    auditoriaService.listar.and.returnValue(of(paginaAuditoria));

    await TestBed.configureTestingModule({
      imports: [DashboardGerenteHomeComponent],
      providers: [
        provideRouter([]),
        { provide: ReporteService, useValue: reporteService },
        { provide: AuthService, useValue: { hasRole: (...roles: string[]) => roles.includes('GERENTE') } },
        { provide: MultaService, useValue: multaService },
        { provide: AuditoriaService, useValue: auditoriaService },
        { provide: ActivatedRoute, useValue: { snapshot: {} } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardGerenteHomeComponent);
    component = fixture.componentInstance;
  });

  it('carga el top 5 de libros más prestados desde ReporteService', () => {
    fixture.detectChanges(); // ngOnInit

    expect(reporteService.librosMasPrestados).toHaveBeenCalled();
    expect(component.librosMasPrestados.length).toBe(5); // Top 5.
    expect(component.librosMasPrestados[0].titulo).toBe('Libro 1');
    expect(component.cargando).toBeFalse();
    expect(component.error).toBe('');
  });

  it('muestra mensaje de error si el reporte de libros falla y deja de cargar', () => {
    reporteService.librosMasPrestados.and.returnValue(throwError(() => ({ status: 403 })));

    fixture.detectChanges();

    expect(component.error).toContain('No se pudo cargar');
    expect(component.cargando).toBeFalse();
    expect(component.librosMasPrestados.length).toBe(0);
  });

  it('carga el resumen financiero desde MultaService', () => {
    fixture.detectChanges();

    expect(multaService.resumenFinanciero).toHaveBeenCalled();
    expect(component.resumenFinanciero).toEqual(resumenFinanciero);
    expect(component.cargandoFinanciero).toBeFalse();
    expect(component.errorFinanciero).toBe('');
  });

  it('muestra mensaje de error si el resumen financiero falla', () => {
    multaService.resumenFinanciero.and.returnValue(throwError(() => ({ status: 403 })));

    fixture.detectChanges();

    expect(component.errorFinanciero).toContain('No se pudo cargar');
    expect(component.cargandoFinanciero).toBeFalse();
    expect(component.resumenFinanciero).toBeNull();
  });

  it('carga la morosidad y deriva cantidad + monto total adeudado (no un porcentaje)', () => {
    fixture.detectChanges();

    expect(reporteService.morosidad).toHaveBeenCalled();
    expect(component.usuariosEnMora.length).toBe(2);
    expect(component.montoTotalAdeudado).toBe(40); // 15.5 + 24.5
    expect(component.cargandoMorosidad).toBeFalse();
  });

  it('muestra mensaje de error si el reporte de morosidad falla', () => {
    reporteService.morosidad.and.returnValue(throwError(() => ({ status: 403 })));

    fixture.detectChanges();

    expect(component.errorMorosidad).toContain('No se pudo cargar');
    expect(component.cargandoMorosidad).toBeFalse();
    expect(component.usuariosEnMora.length).toBe(0);
  });

  it('carga los últimos 5 eventos de auditoría desde AuditoriaService', () => {
    fixture.detectChanges();

    expect(auditoriaService.listar).toHaveBeenCalledWith({ page: 0, size: 5 });
    expect(component.eventosAuditoria.length).toBe(1);
    expect(component.eventosAuditoria[0].accion).toBe('UPDATE');
    expect(component.cargandoAuditoria).toBeFalse();
  });

  it('muestra mensaje de error si la auditoría reciente falla', () => {
    auditoriaService.listar.and.returnValue(throwError(() => ({ status: 403 })));

    fixture.detectChanges();

    expect(component.errorAuditoria).toContain('No se pudo cargar');
    expect(component.cargandoAuditoria).toBeFalse();
    expect(component.eventosAuditoria.length).toBe(0);
  });

  // Misma regresión que el home admin: filas fantasma con 200.
  it('filtra filas fantasma con ids nulos sin romper el render', () => {
    reporteService.librosMasPrestados.and.returnValue(of([
      { libroId: null, titulo: null, isbn: '9788401352836', totalPrestamos: null } as any
    ]));

    expect(() => { fixture.detectChanges(); }).not.toThrow();

    expect(component.librosMasPrestados).toEqual([]);
    expect(component.cargando).toBeFalse();
  });

  it('tolera pagina y pagosRecientes nulos sin romper el render', () => {
    auditoriaService.listar.and.returnValue(of(null as any));
    multaService.resumenFinanciero.and.returnValue(of({
      totalRecaudado: 1, totalPendiente: 2, totalGeneradoHoy: 0, pagosRecientes: null
    } as any));

    expect(() => { fixture.detectChanges(); }).not.toThrow();

    expect(component.eventosAuditoria).toEqual([]);
    expect(component.resumenFinanciero?.pagosRecientes).toEqual([]);
    expect(component.cargandoAuditoria).toBeFalse();
    expect(component.cargandoFinanciero).toBeFalse();
  });

  it('los accesos rápidos apuntan al shell propio (nunca a /dashboard-admin)', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const hrefs = Array.from(compiled.querySelectorAll('a'))
      .map(a => a.getAttribute('routerlink') ?? a.getAttribute('href') ?? '');
    const internos = hrefs.filter(h => h.includes('dashboard'));
    expect(internos.length).toBeGreaterThan(0);
    for (const h of internos) {
      expect(h).toContain('/dashboard-gerente');
      expect(h).not.toContain('/dashboard-admin');
    }
  });
});
