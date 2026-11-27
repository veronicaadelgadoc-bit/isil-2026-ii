package com.isil.edu.pe.spedidos.service.revision;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * PA4 - Utilidades de Arrange/Assert para las pruebas JDBC de revisión (H2).
 */
final class RevisionTestDatos {

  private RevisionTestDatos() {
  }

  static void limpiar(JdbcTemplate jdbc) {
    jdbc.update("DELETE FROM pedido_revision");
    jdbc.update("DELETE FROM pedido");
  }

  static long insertarPedidoPendiente(JdbcTemplate jdbc, String cliente) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement("""
          INSERT INTO pedido (cliente, producto_codigo, producto_nombre, precio_unitario,
                              cantidad, total, fecha, revisado)
          VALUES (?, ?, ?, ?, ?, ?, ?, FALSE)
          """, new String[] {"id"});
      ps.setString(1, cliente);
      ps.setString(2, "LAP-001");
      ps.setString(3, "Laptop");
      ps.setBigDecimal(4, new BigDecimal("1500.00"));
      ps.setInt(5, 2);
      ps.setBigDecimal(6, new BigDecimal("3000.00"));
      ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
      return ps;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  static boolean revisado(JdbcTemplate jdbc, long pedidoId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject("SELECT revisado FROM pedido WHERE id = ?", Boolean.class, pedidoId));
  }

  static int contarRevisiones(JdbcTemplate jdbc, long pedidoId) {
    Integer total = jdbc.queryForObject(
        "SELECT COUNT(*) FROM pedido_revision WHERE pedido_id = ?", Integer.class, pedidoId);
    return total == null ? 0 : total;
  }

  static int contarTodasLasRevisiones(JdbcTemplate jdbc) {
    Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM pedido_revision", Integer.class);
    return total == null ? 0 : total;
  }

  static String observacion(JdbcTemplate jdbc, long pedidoId) {
    return jdbc.queryForObject(
        "SELECT observacion FROM pedido_revision WHERE pedido_id = ?", String.class, pedidoId);
  }
}
