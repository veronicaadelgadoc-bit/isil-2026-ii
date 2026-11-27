package com.isil.edu.pe.spedidos.service.dto;

import java.time.LocalDateTime;

/**
 * PA4 - Resultado de una revisión confirmada (commit).
 */
public record RevisionPedidoResultado(
    long revisionId,
    long pedidoId,
    String observacion,
    LocalDateTime fechaRevision
) { }
