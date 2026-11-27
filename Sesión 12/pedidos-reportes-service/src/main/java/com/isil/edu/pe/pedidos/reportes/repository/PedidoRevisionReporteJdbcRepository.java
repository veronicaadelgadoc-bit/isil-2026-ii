package com.isil.edu.pe.pedidos.reportes.repository;

import com.isil.edu.pe.pedidos.reportes.repository.mapper.PedidoRevisionRowMapper;
import com.isil.edu.pe.pedidos.reportes.repository.projection.ConteoRevisionProjection;
import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevisionEstado;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PA4 - Consultas de solo lectura sobre el estado de revisión de pedidos.
 *
 * <p>SQL parametrizado con {@link NamedParameterJdbcTemplate}; el filtro de estado
 * se envía como parámetro {@code :revisado}, nunca concatenado.</p>
 */
@Repository
public class PedidoRevisionReporteJdbcRepository {

  private static final String SELECT_BASE = """
      SELECT p.id,
             p.cliente,
             p.producto_codigo,
             p.producto_nombre,
             p.cantidad,
             p.total,
             p.fecha,
             p.revisado,
             r.observacion,
             r.fecha_revision
      FROM pedido p
      LEFT JOIN pedido_revision r ON r.pedido_id = p.id
      """;

  static final String SQL_LISTAR_TODOS = SELECT_BASE + " ORDER BY p.id DESC";

  static final String SQL_LISTAR_POR_ESTADO = SELECT_BASE
      + " WHERE p.revisado = :revisado ORDER BY p.id DESC";

  static final String SQL_CONTEO = """
      SELECT COUNT(*) AS total_pedidos,
             COALESCE(SUM(CASE WHEN revisado = TRUE THEN 1 ELSE 0 END), 0) AS total_revisados
      FROM pedido
      """;

  private final NamedParameterJdbcTemplate jdbc;
  private final PedidoRevisionRowMapper rowMapper;

  public PedidoRevisionReporteJdbcRepository(NamedParameterJdbcTemplate jdbc,
      PedidoRevisionRowMapper rowMapper) {
    this.jdbc = jdbc;
    this.rowMapper = rowMapper;
  }

  public List<PedidoRevisionEstado> listarTodos() {
    return jdbc.query(SQL_LISTAR_TODOS, rowMapper);
  }

  public List<PedidoRevisionEstado> listarPorEstado(boolean revisado) {
    return jdbc.query(SQL_LISTAR_POR_ESTADO, Map.of("revisado", revisado), rowMapper);
  }

  public ConteoRevisionProjection contarRevisiones() {
    return jdbc.queryForObject(SQL_CONTEO, Map.of(), (rs, rowNum) ->
        new ConteoRevisionProjection(rs.getLong("total_pedidos"), rs.getLong("total_revisados")));
  }
}
