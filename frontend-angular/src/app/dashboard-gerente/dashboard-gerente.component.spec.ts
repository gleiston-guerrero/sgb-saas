import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DashboardGerenteComponent } from './dashboard-gerente.component';
import { AuthService } from '../core/services/auth.service';

describe('DashboardGerenteComponent (shell propio)', () => {
  let component: DashboardGerenteComponent;
  let fixture: ComponentFixture<DashboardGerenteComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    // El shell persiste colapso en localStorage: aislar cada test del orden de ejecución.
    localStorage.clear();
    authService = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'hasRole', 'logout', 'getCorreo']);
    authService.isLoggedIn.and.returnValue(true);
    authService.hasRole.and.callFake((...roles: string[]) => roles.includes('GERENTE'));
    authService.getCorreo.and.returnValue('gerente@correo.com');

    await TestBed.configureTestingModule({
      imports: [DashboardGerenteComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardGerenteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debería crear el componente', () => {
    expect(component).toBeTruthy();
  });

  it('todas las rutas del sidebar viven bajo /dashboard-gerente (nunca /dashboard-admin)', () => {
    const rutas = component.secciones.flatMap(s => s.enlaces.map(e => e.ruta));
    expect(rutas.length).toBeGreaterThan(0);
    for (const ruta of rutas) {
      expect(ruta.startsWith('/dashboard-gerente')).toBeTrue();
      expect(ruta).not.toContain('/dashboard-admin');
    }
  });

  it('debería mostrar Reportes en el sidebar para GERENTE', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Reportes');
  });

  it('incluye la base operativa de bibliotecario (Préstamos, Reservaciones, Devoluciones, Multas)', () => {
    const etiquetas = component.secciones.flatMap(s => s.enlaces.map(e => e.etiqueta));
    for (const esperada of ['Préstamos', 'Reservaciones', 'Devoluciones', 'Multas', 'Libros']) {
      expect(etiquetas).toContain(esperada);
    }
  });

  it('agrega gestión ampliada (Proveedores, Sugerencias, Mis usuarios) sin opciones de ADMIN', () => {
    const etiquetas = component.secciones.flatMap(s => s.enlaces.map(e => e.etiqueta));
    for (const esperada of ['Proveedores', 'Sugerencias', 'Mis usuarios', 'Reportes']) {
      expect(etiquetas).toContain(esperada);
    }
    for (const ausente of ['Auditoría', 'Configuración', 'Usuarios']) {
      expect(etiquetas).not.toContain(ausente);
    }
  });

  it('debería mostrar nombre del rol como Gerente', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Gerente');
  });
});
