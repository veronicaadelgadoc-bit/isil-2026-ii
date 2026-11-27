package com.isil.edu.pe.spedidos.exception;

/**
 * PA4 - El pedido ya tiene una revisión confirmada (HTTP 409 Conflict).
 */
public class PedidoYaRevisadoException extends PedidoException {

  public PedidoYaRevisadoException(long pedidoId) {
    super("El pedido " + pedidoId + " ya fue revisado. No se registró una segunda revisión.");
  }

  public PedidoYaRevisadoException(long pedidoId, Throwable cause) {
    super("El pedido " + pedidoId + " ya fue revisado. No se registró una segunda revisión.", cause);
  }
}
