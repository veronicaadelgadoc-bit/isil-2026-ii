package com.isil.edu.pe.pedidos.reportes.exception;

/**
 * PA4 - Valor no reconocido en {@code ?estado=} (HTTP 400).
 */
public class EstadoRevisionInvalidoException extends RuntimeException {

  public EstadoRevisionInvalidoException(String valor) {
    super("Estado de revisión no válido: '" + valor + "'. Valores permitidos: PENDIENTE, REVISADO.");
  }
}
