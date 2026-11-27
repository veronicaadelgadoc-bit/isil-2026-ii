package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.entity.PedidoRevision;
import com.isil.edu.pe.spedidos.exception.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.repository.PedidoRevisionJdbcRepository;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PA4 - Implementación transaccional de la revisión de pedidos.
 *
 * <pre>
 * revisarPedido(id, observacion)                 &lt;- @Transactional (BEGIN)
 *   1. validar entrada (sin tocar la BD)
 *   2. UPDATE pedido SET revisado = TRUE WHERE id = ? AND revisado = FALSE
 *        0 filas -> ¿existe? no: 404 / sí: 409   (RuntimeException -> ROLLBACK)
 *   3. INSERT INTO pedido_revision (...)
 *        falla SQL -> DataAccessException      (RuntimeException -> ROLLBACK)
 *   4. retorno normal                          -> COMMIT
 * </pre>
 *
 * <p><strong>Frontera transaccional:</strong> este método NO invoca a
 * {@code inventario-service} ni a {@code pedidos-reportes-service}. La transacción
 * es local a MySQL y solo contiene las dos escrituras JDBC.</p>
 */
@Service
public class PedidoRevisionServiceImpl implements PedidoRevisionService {

  private static final Logger log = LoggerFactory.getLogger(PedidoRevisionServiceImpl.class);

  private final PedidoRevisionJdbcRepository revisionRepository;

  public PedidoRevisionServiceImpl(PedidoRevisionJdbcRepository revisionRepository) {
    this.revisionRepository = revisionRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public RevisionPedidoResultado revisarPedido(Long pedidoId, String observacion) {
    validarEntrada(pedidoId, observacion);
    String observacionNormalizada = observacion.trim();

    // Escritura 1: UPDATE condicionado (solo pedidos pendientes).
    int filasActualizadas = revisionRepository.marcarComoRevisado(pedidoId);
    if (filasActualizadas != 1) {
      if (!revisionRepository.existePedido(pedidoId)) {
        throw new PedidoNoEncontradoException(pedidoId);
      }
      throw new PedidoYaRevisadoException(pedidoId);
    }

    // Escritura 2: INSERT de trazabilidad. Si falla, se revierte también el UPDATE.
    LocalDateTime fechaRevision = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    long revisionId;
    try {
      revisionId = revisionRepository.insertarRevision(pedidoId, observacionNormalizada, fechaRevision);
    } catch (DuplicateKeyException e) {
      // Respaldo de la restricción UNIQUE(pedido_id): nunca dos revisiones confirmadas.
      throw new PedidoYaRevisadoException(pedidoId, e);
    }

    log.info("Pedido {} revisado (revisión {}).", pedidoId, revisionId);
    return new RevisionPedidoResultado(revisionId, pedidoId, observacionNormalizada, fechaRevision);
  }

  private void validarEntrada(Long pedidoId, String observacion) {
    if (pedidoId == null || pedidoId <= 0) {
      throw new RevisionInvalidaException("El ID del pedido debe ser un número mayor que cero.");
    }
    if (observacion == null || observacion.isBlank()) {
      throw new RevisionInvalidaException("La observación de la revisión es obligatoria.");
    }
    if (observacion.trim().length() > PedidoRevision.OBSERVACION_MAX) {
      throw new RevisionInvalidaException(
          "La observación no puede superar " + PedidoRevision.OBSERVACION_MAX + " caracteres.");
    }
  }
}
