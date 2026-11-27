package com.isil.edu.pe.spedidos.client.reportes.dto;

import java.math.BigDecimal;

/**
 * PA4 - DTO de transporte para {@code GET /api/v1/pedidos/estadisticas/revisiones}.
 */
public record EstadisticaRevisionResponse(
    long totalPedidos,
    long totalRevisados,
    long totalPendientes,
    BigDecimal porcentajeRevisado
) { }
