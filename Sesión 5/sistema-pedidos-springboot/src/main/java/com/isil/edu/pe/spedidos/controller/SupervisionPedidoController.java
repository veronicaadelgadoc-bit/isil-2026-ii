package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.client.reportes.EstadoRevision;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteClient;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteNoDisponibleException;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteSolicitudInvalidaException;
import com.isil.edu.pe.spedidos.exception.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * PA4 - Controller MVC de supervisión y revisión de pedidos.
 *
 * <pre>
 * Lectura (GET /supervision/pedidos):
 *   Controller -> PedidoReporteClient -> HTTP -> pedidos-reportes-service
 *
 * Escritura (POST /supervision/pedidos/{id}/revisar):
 *   Controller -> PedidoRevisionService (@Transactional) -> PedidoRevisionJdbcRepository -> MySQL
 *   éxito -> redirect:/supervision/pedidos (Post/Redirect/Get)
 * </pre>
 *
 * <p>El Controller solo recibe parámetros, delega y construye el {@link Model}.
 * No contiene SQL ni reglas de negocio.</p>
 */
@Controller
@RequestMapping("/supervision/pedidos")
public class SupervisionPedidoController {

  static final String VISTA = "supervision-pedidos";

  private static final Logger log = LoggerFactory.getLogger(SupervisionPedidoController.class);

  private final PedidoRevisionService pedidoRevisionService;
  private final PedidoReporteClient pedidoReporteClient;

  public SupervisionPedidoController(PedidoRevisionService pedidoRevisionService,
      PedidoReporteClient pedidoReporteClient) {
    this.pedidoRevisionService = pedidoRevisionService;
    this.pedidoReporteClient = pedidoReporteClient;
  }

  /**
   * Muestra los pedidos con su estado de revisión y el resumen estadístico,
   * ambos obtenidos desde pedidos-reportes-service.
   */
  @GetMapping
  public String listar(@RequestParam(name = "estado", required = false) String estado,
      Model model, HttpServletResponse response) {

    EstadoRevision filtro;
    try {
      filtro = EstadoRevision.desdeTexto(estado).orElse(null);
    } catch (IllegalArgumentException e) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute("error", "El estado indicado no es válido. Use PENDIENTE, REVISADO o TODOS.");
      filtro = null;
    }

    cargarDatosSupervision(model, response, filtro);
    return VISTA;
  }

  /**
   * Revisa un pedido. Éxito: PRG. Error de negocio: misma vista con código HTTP coherente.
   */
  @PostMapping("/{id}/revisar")
  public String revisar(@PathVariable("id") String id,
      @RequestParam(name = "observacion", required = false) String observacion,
      Model model, HttpServletResponse response, RedirectAttributes redirectAttributes) {

    Long pedidoId;
    try {
      pedidoId = Long.valueOf(id.trim());
    } catch (NumberFormatException e) {
      return mostrarError(model, response, HttpStatus.BAD_REQUEST,
          "El ID del pedido debe ser un número entero mayor que cero.", observacion);
    }

    try {
      RevisionPedidoResultado resultado = pedidoRevisionService.revisarPedido(pedidoId, observacion);
      redirectAttributes.addFlashAttribute("mensaje",
          "Pedido " + resultado.pedidoId() + " revisado correctamente (revisión N.° "
              + resultado.revisionId() + ").");
      redirectAttributes.addFlashAttribute("pedidoRevisadoId", resultado.pedidoId());
      return "redirect:/supervision/pedidos";
    } catch (RevisionInvalidaException e) {
      return mostrarError(model, response, HttpStatus.BAD_REQUEST, e.getMessage(), observacion);
    } catch (PedidoNoEncontradoException e) {
      return mostrarError(model, response, HttpStatus.NOT_FOUND, e.getMessage(), observacion);
    } catch (PedidoYaRevisadoException e) {
      return mostrarError(model, response, HttpStatus.CONFLICT, e.getMessage(), observacion);
    } catch (DataAccessException e) {
      log.error("Error de base de datos al revisar el pedido {}.", pedidoId, e);
      return mostrarError(model, response, HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo registrar la revisión. No se guardaron cambios; intente nuevamente.", observacion);
    }
  }

  private String mostrarError(Model model, HttpServletResponse response, HttpStatus status,
      String mensaje, String observacion) {
    response.setStatus(status.value());
    model.addAttribute("error", mensaje);
    model.addAttribute("observacionIngresada", observacion);
    cargarDatosSupervision(model, response, null);
    return VISTA;
  }

  /**
   * Obtiene listado y resumen vía HTTP. Si el servicio no responde, la vista se muestra
   * con un aviso (503) y sin trazas técnicas; la base local no se toca.
   */
  private void cargarDatosSupervision(Model model, HttpServletResponse response, EstadoRevision filtro) {
    model.addAttribute("estado", Optional.ofNullable(filtro).map(Enum::name).orElse("TODOS"));
    try {
      model.addAttribute("pedidos", pedidoReporteClient.listarRevisiones(filtro));
      model.addAttribute("estadisticas", pedidoReporteClient.obtenerEstadisticasRevision());
      model.addAttribute("servicioReportesDisponible", true);
    } catch (PedidoReporteNoDisponibleException | PedidoReporteSolicitudInvalidaException e) {
      log.warn("Supervisión sin datos de reportes: {}", e.getMessage());
      if (response.getStatus() < 400) {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
      }
      model.addAttribute("pedidos", List.of());
      model.addAttribute("servicioReportesDisponible", false);
      model.addAttribute("errorServicio", e.getMessage());
    }
  }
}
