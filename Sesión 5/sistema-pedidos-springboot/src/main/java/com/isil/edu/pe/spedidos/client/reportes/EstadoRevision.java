package com.isil.edu.pe.spedidos.client.reportes;

import java.util.Locale;
import java.util.Optional;

/**
 * PA4 - Filtro de estado de revisión aceptado por
 * {@code GET /api/v1/pedidos/revisiones?estado=}.
 */
public enum EstadoRevision {
  PENDIENTE,
  REVISADO;

  /**
   * Convierte el texto recibido desde la vista. Vacío o "TODOS" = sin filtro.
   *
   * @return estado reconocido; {@code Optional.empty()} si no se filtra
   * @throws IllegalArgumentException si el valor no es reconocido
   */
  public static Optional<EstadoRevision> desdeTexto(String valor) {
    if (valor == null || valor.isBlank() || "TODOS".equalsIgnoreCase(valor.trim())) {
      return Optional.empty();
    }
    return Optional.of(EstadoRevision.valueOf(valor.trim().toUpperCase(Locale.ROOT)));
  }
}
