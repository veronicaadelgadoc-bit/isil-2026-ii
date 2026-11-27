package com.isil.edu.pe.spedidos.repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * PA4 - Repositorio Spring JDBC del módulo de revisión de pedidos.
 *
 * <p>Contiene las dos escrituras que forman la transacción de revisión:</p>
 * <ol>
 *   <li>{@link #marcarComoRevisado(long)}: {@code UPDATE pedido SET revisado = TRUE ...}</li>
 *   <li>{@link #insertarRevision(long, String, LocalDateTime)}: {@code INSERT INTO pedido_revision ...}</li>
 * </ol>
 *
 * <p>Todas las sentencias usan parámetros posicionales ({@code ?}); ningún valor
 * ingresado por el usuario se concatena en el SQL. El repositorio no abre ni
 * confirma transacciones: participa en la transacción iniciada por
 * {@code PedidoRevisionServiceImpl} mediante {@code @Transactional}.</p>
 */
@Repository
public class PedidoRevisionJdbcRepository {

  static final String SQL_MARCAR_REVISADO =
      "UPDATE pedido SET revisado = TRUE WHERE id = ? AND revisado = FALSE";

  static final String SQL_INSERTAR_REVISION =
      "INSERT INTO pedido_revision (pedido_id, observacion, fecha_revision) VALUES (?, ?, ?)";

  static final String SQL_EXISTE_PEDIDO =
      "SELECT COUNT(*) FROM pedido WHERE id = ?";

  protected final JdbcTemplate jdbcTemplate;

  public PedidoRevisionJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Marca el pedido como revisado solo si todavía está pendiente.
   *
   * <p>La condición {@code revisado = FALSE} hace que la operación sea atómica:
   * si dos supervisores revisan el mismo pedido a la vez, solo uno obtiene 1 fila
   * afectada.</p>
   *
   * @return filas afectadas: 1 si se marcó; 0 si no existe o ya estaba revisado
   */
  public int marcarComoRevisado(long pedidoId) {
    return jdbcTemplate.update(SQL_MARCAR_REVISADO, pedidoId);
  }

  /**
   * Inserta la evidencia de la revisión y devuelve el ID generado.
   */
  public long insertarRevision(long pedidoId, String observacion, LocalDateTime fechaRevision) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(SQL_INSERTAR_REVISION, new String[] {"id"});
      ps.setLong(1, pedidoId);
      ps.setString(2, observacion);
      ps.setTimestamp(3, Timestamp.valueOf(fechaRevision));
      return ps;
    }, keyHolder);

    Number clave = keyHolder.getKey();
    return clave != null ? clave.longValue() : 0L;
  }

  /**
   * Indica si existe un pedido con el ID indicado (para distinguir 404 de 409).
   */
  public boolean existePedido(long pedidoId) {
    Integer total = jdbcTemplate.queryForObject(SQL_EXISTE_PEDIDO, Integer.class, pedidoId);
    return total != null && total > 0;
  }
}
