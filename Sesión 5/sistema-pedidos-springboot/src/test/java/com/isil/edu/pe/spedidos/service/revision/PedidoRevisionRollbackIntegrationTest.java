package com.isil.edu.pe.spedidos.service.revision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isil.edu.pe.spedidos.repository.PedidoRevisionJdbcRepository;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * PA4 - Caso 4 "Rollback real".
 *
 * <p>Se reemplaza el repositorio por una variante cuyo {@code insertarRevision}
 * ejecuta un INSERT real que la base rechaza ({@code observacion = NULL} viola
 * {@code NOT NULL}). El UPDATE de {@code pedido.revisado} se ejecuta de verdad
 * antes del fallo; la prueba comprueba que la transacción lo revierte.</p>
 *
 * <p>Usa una base H2 propia para no interferir con otras clases de prueba.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PedidoRevisionRollbackIntegrationTest.FallaEnInsertConfig.class)
@TestPropertySource(properties =
    "spring.datasource.url=jdbc:h2:mem:pedidosdb_rollback;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class PedidoRevisionRollbackIntegrationTest {

  @Autowired
  private PedidoRevisionService pedidoRevisionService;

  @Autowired
  private RepositorioConFallaEnInsert repositorioConFalla;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void prepararBase() {
    RevisionTestDatos.limpiar(jdbcTemplate);
    repositorioConFalla.reiniciar();
  }

  @AfterEach
  void limpiarBase() {
    RevisionTestDatos.limpiar(jdbcTemplate);
  }

  @Test
  @DisplayName("Caso 4 - Falla SQL en INSERT de pedido_revision revierte el UPDATE previo de pedido")
  void revertirUpdateCuandoFallaElInsertDeTrazabilidad() {
    // Arrange
    long pedidoId = RevisionTestDatos.insertarPedidoPendiente(jdbcTemplate, "Jorge Salas");
    assertFalse(RevisionTestDatos.revisado(jdbcTemplate, pedidoId));

    // Act
    assertThrows(DataIntegrityViolationException.class,
        () -> pedidoRevisionService.revisarPedido(pedidoId, "Observación válida"));

    // Assert: el UPDATE sí se ejecutó (1 fila) dentro de la transacción...
    assertEquals(1, repositorioConFalla.filasActualizadasEnUpdate());
    assertTrue(repositorioConFalla.insertIntentado());
    // ...pero el rollback dejó revisado en su valor anterior y sin revisión confirmada.
    assertFalse(RevisionTestDatos.revisado(jdbcTemplate, pedidoId));
    assertEquals(0, RevisionTestDatos.contarRevisiones(jdbcTemplate, pedidoId));
  }

  @TestConfiguration
  static class FallaEnInsertConfig {

    @Bean
    @Primary
    RepositorioConFallaEnInsert repositorioConFallaEnInsert(JdbcTemplate jdbcTemplate) {
      return new RepositorioConFallaEnInsert(jdbcTemplate);
    }
  }

  /**
   * Repositorio de prueba: UPDATE real + INSERT real que la base rechaza.
   */
  static class RepositorioConFallaEnInsert extends PedidoRevisionJdbcRepository {

    private int filasActualizadas = -1;
    private boolean insertIntentado;

    RepositorioConFallaEnInsert(JdbcTemplate jdbcTemplate) {
      super(jdbcTemplate);
    }

    @Override
    public int marcarComoRevisado(long pedidoId) {
      filasActualizadas = super.marcarComoRevisado(pedidoId);
      return filasActualizadas;
    }

    @Override
    public long insertarRevision(long pedidoId, String observacion, LocalDateTime fechaRevision) {
      insertIntentado = true;
      // observacion = NULL -> viola NOT NULL -> DataIntegrityViolationException (RuntimeException)
      jdbcTemplate.update(
          "INSERT INTO pedido_revision (pedido_id, observacion, fecha_revision) VALUES (?, NULL, ?)",
          pedidoId, Timestamp.valueOf(fechaRevision));
      return 0L;
    }

    public int filasActualizadasEnUpdate() {
      return filasActualizadas;
    }

    public boolean insertIntentado() {
      return insertIntentado;
    }

    public void reiniciar() {
      filasActualizadas = -1;
      insertIntentado = false;
    }
  }
}
