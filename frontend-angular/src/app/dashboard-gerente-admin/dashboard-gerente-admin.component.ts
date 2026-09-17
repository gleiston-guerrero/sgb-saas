import { Component } from '@angular/core';
import { DashboardShellComponent } from '../shared/dashboard-shell/dashboard-shell.component';
import { SeccionSidebar } from '../shared/dashboard-shell/seccion-sidebar.model';

// Shell exclusivo de ADMIN. GERENTE usa su propio shell en
// /dashboard-gerente (DashboardGerenteComponent). Ruta y guards intactos.
@Component({
  selector: 'app-dashboard-gerente-admin',
  standalone: true,
  imports: [DashboardShellComponent],
  templateUrl: './dashboard-gerente-admin.component.html',
  styles: [`:host { display: block; height: 100%; overflow: hidden; }`]
})
export class DashboardGerenteAdminComponent {
  // Espejo de los @PreAuthorize reales verificados en backend-springboot:
  // - /libros, /prestamos/gestion, /reservaciones, /devoluciones, /multas:
  //   BIBLIOTECARIO/GERENTE/ADMIN (LoanController, BookController...).
  // - /proveedores, /sugerencias/gestion, /reportes: GERENTE/ADMIN.
  // - /admin/usuarios, /auditoria, /admin/configuracion: ADMIN
  //   (UserAdminController admite GERENTE pero limitado a sus creados: esa
  //   vista vive como /dashboard-gerente/admin/mis-usuarios).
  secciones: SeccionSidebar[] = [
    {
      titulo: 'INICIO',
      enlaces: [
        { ruta: '/dashboard-admin', etiqueta: 'Inicio', icono: 'home', roles: ['ADMIN'] },
      ]
    },
    {
       titulo: 'GESTIÓN',
      enlaces: [
        { ruta: '/dashboard-admin/libros', etiqueta: 'Libros', icono: 'inventory_2', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/libros-pendientes', etiqueta: 'Pendientes', icono: 'pending_actions', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/prestamos/gestion', etiqueta: 'Préstamos', icono: 'menu_book', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/reservaciones', etiqueta: 'Reservaciones', icono: 'event_available', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/devoluciones', etiqueta: 'Devoluciones', icono: 'assignment_return', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/multas', etiqueta: 'Multas', icono: 'payments', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/proveedores', etiqueta: 'Proveedores', icono: 'local_shipping', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/sugerencias/gestion', etiqueta: 'Sugerencias', icono: 'lightbulb', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/admin/usuarios', etiqueta: 'Usuarios', icono: 'manage_accounts', roles: ['ADMIN'] },
      ]
    },
    {
      titulo: 'SISTEMA',
      enlaces: [
        { ruta: '/dashboard-admin/auditoria', etiqueta: 'Auditoría', icono: 'receipt_long', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/reportes', etiqueta: 'Reportes', icono: 'bar_chart', roles: ['ADMIN'] },
        { ruta: '/dashboard-admin/admin/configuracion', etiqueta: 'Configuración', icono: 'settings', roles: ['ADMIN'] },
      ]
    }
  ];
}
