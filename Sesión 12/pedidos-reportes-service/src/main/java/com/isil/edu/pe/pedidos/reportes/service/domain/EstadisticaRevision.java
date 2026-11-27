package com.isil.edu.pe.pedidos.reportes.service.domain;

import java.math.BigDecimal;

/**
 * PA4 - Resumen estadístico de revisiones calculado en la capa Service.
 */
public record EstadisticaRevision(
    long totalPedidos,
    long totalRevisados,
    long totalPendientes,
    BigDecimal porcentajeRevisado
) { }
