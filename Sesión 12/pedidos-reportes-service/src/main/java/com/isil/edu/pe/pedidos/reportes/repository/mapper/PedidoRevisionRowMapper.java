package com.isil.edu.pe.pedidos.reportes.repository.mapper;

import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevisionEstado;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * PA4 - Convierte una fila de {@code pedido LEFT JOIN pedido_revision}.
 */
@Component
public class PedidoRevisionRowMapper implements RowMapper<PedidoRevisionEstado> {

  @Override
  public PedidoRevisionEstado mapRow(ResultSet rs, int rowNum) throws SQLException {
    boolean revisado = rs.getBoolean("revisado");
    return new PedidoRevisionEstado(
        rs.getLong("id"),
        rs.getString("cliente"),
        rs.getString("producto_codigo"),
        rs.getString("producto_nombre"),
        rs.getInt("cantidad"),
        rs.getBigDecimal("total"),
        aFecha(rs.getTimestamp("fecha")),
        revisado,
        revisado ? rs.getString("observacion") : null,
        revisado ? aFecha(rs.getTimestamp("fecha_revision")) : null
    );
  }

  private LocalDateTime aFecha(Timestamp timestamp) {
    return timestamp != null ? timestamp.toLocalDateTime() : null;
  }
}
