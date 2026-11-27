package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.exception.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;

/**
 * PA4 - Caso de uso "revisión de pedido".
 *
 * <p>Marca un pedido como revisado y registra su trazabilidad en
 * {@code pedido_revision}. Ambas escrituras se confirman juntas o se revierten
 * juntas (transacción local Spring JDBC sobre MySQL).</p>
 */
public interface PedidoRevisionService {

  /**
   * Revisa un pedido pendiente.
   *
   * @param pedidoId    ID del pedido (debe ser mayor que cero)
   * @param observacion observación del supervisor (obligatoria, máx. 500 caracteres)
   * @return datos de la revisión confirmada
   * @throws RevisionInvalidaException   si los parámetros no son válidos (400)
   * @throws PedidoNoEncontradoException si el pedido no existe (404)
   * @throws PedidoYaRevisadoException   si el pedido ya tiene una revisión (409)
   */
  RevisionPedidoResultado revisarPedido(Long pedidoId, String observacion);
}
