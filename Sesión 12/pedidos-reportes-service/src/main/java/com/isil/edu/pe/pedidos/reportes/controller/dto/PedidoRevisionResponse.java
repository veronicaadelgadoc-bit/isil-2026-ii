package com.isil.edu.pe.pedidos.reportes.controller.dto;

import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevisionEstado;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PA4 - Contrato JSON de {@code GET /api/v1/pedidos/revisiones}.
 * {@code observacion} y {@code fechaRevision} solo tienen valor cuando el pedido está REVISADO.
 */
public record PedidoRevisionResponse(
    Long id,
    String cliente,
    String productoCodigo,
    String productoNombre,
    int cantidad,
    BigDecimal total,
    LocalDateTime fecha,
    String estadoRevision,
    boolean revisado,
    String observacion,
    LocalDateTime fechaRevision
) {

  public static PedidoRevisionResponse from(PedidoRevisionEstado p) {
    return new PedidoRevisionResponse(
        p.id(), p.cliente(), p.productoCodigo(), p.productoNombre(), p.cantidad(), p.total(),
        p.fecha(), p.estado().name(), p.revisado(), p.observacion(), p.fechaRevision());
  }
}
