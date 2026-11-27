package com.isil.edu.pe.pedidos.reportes.service.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PA4 - Pedido con su estado de revisión (observación y fecha solo si fue revisado).
 */
public record PedidoRevisionEstado(
    Long id,
    String cliente,
    String productoCodigo,
    String productoNombre,
    int cantidad,
    BigDecimal total,
    LocalDateTime fecha,
    boolean revisado,
    String observacion,
    LocalDateTime fechaRevision
) {

  public EstadoRevision estado() {
    return revisado ? EstadoRevision.REVISADO : EstadoRevision.PENDIENTE;
  }
}
