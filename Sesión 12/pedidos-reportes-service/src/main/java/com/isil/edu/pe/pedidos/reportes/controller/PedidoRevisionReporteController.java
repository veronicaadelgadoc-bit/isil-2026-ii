package com.isil.edu.pe.pedidos.reportes.controller;

import com.isil.edu.pe.pedidos.reportes.controller.dto.EstadisticaRevisionDto;
import com.isil.edu.pe.pedidos.reportes.controller.dto.PedidoRevisionResponse;
import com.isil.edu.pe.pedidos.reportes.service.PedidoRevisionReporteService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PA4 - Recursos REST nuevos del módulo de revisión.
 *
 * <ul>
 *   <li>{@code GET /api/v1/pedidos/revisiones?estado=PENDIENTE|REVISADO} → 200 JSON / 400</li>
 *   <li>{@code GET /api/v1/pedidos/estadisticas/revisiones} → 200 JSON</li>
 * </ul>
 *
 * <p>El Controller solo traduce HTTP ↔ DTO; consultas y cálculos están en Service/Repository.</p>
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoRevisionReporteController {

  private final PedidoRevisionReporteService revisionService;

  public PedidoRevisionReporteController(PedidoRevisionReporteService revisionService) {
    this.revisionService = revisionService;
  }

  @GetMapping(value = "/revisiones", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<PedidoRevisionResponse>> listarRevisiones(
      @RequestParam(name = "estado", required = false) String estado) {
    List<PedidoRevisionResponse> body = revisionService.listarPorEstado(estado)
        .stream()
        .map(PedidoRevisionResponse::from)
        .toList();
    return ResponseEntity.ok(body);
  }

  @GetMapping(value = "/estadisticas/revisiones", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EstadisticaRevisionDto> obtenerEstadisticasRevision() {
    return ResponseEntity.ok(EstadisticaRevisionDto.from(revisionService.obtenerEstadisticas()));
  }
}
