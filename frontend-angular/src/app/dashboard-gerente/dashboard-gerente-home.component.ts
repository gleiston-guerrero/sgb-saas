import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReporteService, LibroMasPrestado, ReporteMorosidad } from '../core/services/reporte-gerencial.service';
import { AuthService } from '../core/services/auth.service';
import { MultaService } from '../core/services/multa.service';
import { ResumenFinancieroMultas } from '../core/models/multa.model';
import { AuditoriaService } from '../core/services/auditoria.service';
import { EventoAuditoria } from '../core/models/evento-auditoria.model';

// Home del shell propio GERENTE (ruta '' de /dashboard-gerente):
// bienvenida, libros más prestados, accesos rápidos, resumen
// financiero, morosidad y actividad reciente. Todos los endpoints
// usados admiten GERENTE (verificados en backend: LoanController
// reportes GERENTE/ADMIN, FineController resumen GERENTE/ADMIN,
// AuditController GERENTE/ADMIN).
@Component({
  selector: 'app-dashboard-gerente-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-gerente-home.component.html'
})
export class DashboardGerenteHomeComponent implements OnInit {
  librosMasPrestados: LibroMasPrestado[] = [];
  cargando = true;
  error = '';

  // Resumen financiero (multas): recaudado vs pendiente de cobro.
  resumenFinanciero: ResumenFinancieroMultas | null = null;
  cargandoFinanciero = true;
  errorFinanciero = '';

  // Morosidad: el endpoint devuelve usuarios con multas pendientes; acá se deriva cantidad + monto total.
  usuariosEnMora: ReporteMorosidad[] = [];
  cargandoMorosidad = true;
  errorMorosidad = '';

  // Actividad de auditoría reciente: últimos 5 eventos (lista compacta,
  // no la tabla completa -- para eso está /auditoria en ADMIN).
  eventosAuditoria: EventoAuditoria[] = [];
  cargandoAuditoria = true;
  errorAuditoria = '';

  constructor(
    private reporteService: ReporteService,
    private authService: AuthService,
    private multaService: MultaService,
    private auditoriaService: AuditoriaService
  ) {}

  ngOnInit(): void {
    // El shell /dashboard-gerente tiene roleGuard(['GERENTE']): los
    // reportes siempre se disparan (sin ramas por ADMIN).
    this.reporteService.librosMasPrestados().subscribe({
      next: (libros) => {
        this.librosMasPrestados = (libros ?? []).filter(l => l?.libroId != null).slice(0, 5); // Top 5.
        this.cargando = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el reporte de libros más prestados.';
        this.cargando = false;
      }
    });

    this.reporteService.morosidad().subscribe({
      next: (res: any) => {
        const usuarios = Array.isArray(res) ? res : (res?.content ?? []);
        this.usuariosEnMora = usuarios;
        this.cargandoMorosidad = false;
      },
      error: () => {
        this.errorMorosidad = 'No se pudo cargar el reporte de morosidad.';
        this.cargandoMorosidad = false;
      }
    });

    this.multaService.resumenFinanciero().subscribe({
      next: (resumen) => {
        this.resumenFinanciero = resumen
          ? { ...resumen, pagosRecientes: resumen.pagosRecientes ?? [] }
          : resumen;
        this.cargandoFinanciero = false;
      },
      error: () => {
        this.errorFinanciero = 'No se pudo cargar el resumen financiero.';
        this.cargandoFinanciero = false;
      }
    });

    this.auditoriaService.listar({ page: 0, size: 5 }).subscribe({
      next: (pagina) => {
        this.eventosAuditoria = pagina?.content ?? [];
        this.cargandoAuditoria = false;
      },
      error: () => {
        this.errorAuditoria = 'No se pudo cargar la actividad de auditoría.';
        this.cargandoAuditoria = false;
      }
    });
  }

  get montoTotalAdeudado(): number {
    return this.usuariosEnMora.reduce((suma, u) => suma + (u.montoTotalAdeudado ?? 0), 0);
  }

  get tituloBienvenida(): string {
    return 'Bienvenida, Gerencia';
  }
}
