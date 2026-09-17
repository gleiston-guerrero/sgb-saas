import { Component } from '@angular/core';
import { DashboardShellComponent } from '../shared/dashboard-shell/dashboard-shell.component';
import { SeccionSidebar } from '../shared/dashboard-shell/seccion-sidebar.model';

// Shell propio del GERENTE: igual base operativa que BIBLIOTECARIO
// (libros, pendientes, préstamos, reservaciones, devoluciones, multas)
// más gestión ampliada (proveedores, sugerencias, mis usuarios) y
// reportes. Menos que ADMIN: sin auditoría, sin usuarios global y sin
// configuración. Espejo de los @PreAuthorize reales del backend.
@Component({
  selector: 'app-dashboard-gerente',
  standalone: true,
  imports: [DashboardShellComponent],
  templateUrl: './dashboard-gerente.component.html',
  styles: [`:host { display: block; height: 100%; overflow: hidden; }`]
})
export class DashboardGerenteComponent {
  secciones: SeccionSidebar[] = [
    {
      titulo: 'INICIO',
      enlaces: [
        { ruta: '/dashboard-gerente', etiqueta: 'Inicio', icono: 'home', roles: ['GERENTE'] },
      ]
    },
    {
      titulo: 'GESTIÓN',
      enlaces: [
        { ruta: '/dashboard-gerente/libros', etiqueta: 'Libros', icono: 'inventory_2', roles: ['GERENTE'] },
        { ruta: '/dashboard-gerente/libros-pendientes', etiqueta: 'Pendientes', icono: 'pending_actions', roles: ['GERENTE'] },
        { ruta: '/dashboard-gerente/prestamos/gestion', etiqueta: 'Préstamos', icono: 'menu_book', roles: ['GERENTE'] },
        { ruta: '/dashboard-gerente/reservaciones', etiqueta: 'Reservaciones', icono: 'event_available', roles: ['GERENTE'] },
        { ruta: '/dashboard-gerente/devoluciones', etiqueta: 'Devoluciones', icono: 'assignment_return', roles: ['GERENTE'] },
        { ruta: '/dashboard-gerente/multas', etiqueta: 'Multas', icono: 'payments', roles: ['GERENTE'] },
        { ruta: '/dashboard-gerente/proveedores', etiqueta: 'Proveedores', icono: 'local_shipping', roles: ['GERENTE'] },
        { ruta: '/dashboard-gerente/sugerencias/gestion', etiqueta: 'Sugerencias', icono: 'lightbulb', roles: ['GERENTE'] },
        { ruta: '/dashboard-gerente/admin/mis-usuarios', etiqueta: 'Mis usuarios', icono: 'group', roles: ['GERENTE'] },
      ]
    },
    {
      titulo: 'SISTEMA',
      enlaces: [
        { ruta: '/dashboard-gerente/reportes', etiqueta: 'Reportes', icono: 'bar_chart', roles: ['GERENTE'] },
      ]
    }
  ];
}