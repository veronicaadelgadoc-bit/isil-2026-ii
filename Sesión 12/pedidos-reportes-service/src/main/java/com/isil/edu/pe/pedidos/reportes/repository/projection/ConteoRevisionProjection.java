package com.isil.edu.pe.pedidos.reportes.repository.projection;

/**
 * PA4 - Conteos crudos obtenidos con SQL (sin cálculos derivados).
 */
public record ConteoRevisionProjection(
    long totalPedidos,
    long totalRevisados
) { }
