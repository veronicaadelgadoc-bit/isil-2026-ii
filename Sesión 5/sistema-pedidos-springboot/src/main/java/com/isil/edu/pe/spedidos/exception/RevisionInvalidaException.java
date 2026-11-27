package com.isil.edu.pe.spedidos.exception;

/**
 * PA4 - Datos de entrada inválidos para revisar un pedido (HTTP 400).
 */
public class RevisionInvalidaException extends PedidoException {

  public RevisionInvalidaException(String message) {
    super(message);
  }
}
