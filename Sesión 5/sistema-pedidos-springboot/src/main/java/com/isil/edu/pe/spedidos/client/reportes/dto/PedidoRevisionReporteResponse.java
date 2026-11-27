package com.isil.edu.pe.spedidos.client.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PA4 - DTO de transporte para {@code GET /api/v1/pedidos/revisiones}.
 *
 * <p>Es un contrato HTTP propio del cliente: no comparte la entidad JPA
 * {@code Pedido} con {@code pedidos-reportes-service}.</p>
 */
public record PedidoRevisionReporteResponse(
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
) { }
