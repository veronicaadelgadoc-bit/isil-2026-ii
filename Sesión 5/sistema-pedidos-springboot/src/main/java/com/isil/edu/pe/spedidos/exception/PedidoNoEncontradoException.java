package com.isil.edu.pe.spedidos.exception;

/**
 * PA4 - El pedido solicitado no existe en la base local (HTTP 404).
 */
public class PedidoNoEncontradoException extends PedidoException {

  public PedidoNoEncontradoException(long pedidoId) {
    super("No existe un pedido registrado con ID " + pedidoId + ".");
  }
}
