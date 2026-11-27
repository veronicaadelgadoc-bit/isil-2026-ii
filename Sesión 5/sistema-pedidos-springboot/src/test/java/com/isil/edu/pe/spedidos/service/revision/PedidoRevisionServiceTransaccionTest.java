package com.isil.edu.pe.spedidos.service.revision;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.isil.edu.pe.spedidos.exception.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * PA4 - Pruebas de integración (H2) del límite transaccional de la revisión.
 *
 * <p>La clase NO es {@code @Transactional}: cada operación del servicio abre y
 * confirma/revierte su propia transacción, igual que en ejecución real. Así se
 * verifica el commit/rollback efectivo consultando la BD después de la llamada.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class PedidoRevisionServiceTransaccionTest {

  @Autowired
  private PedidoRevisionService pedidoRevisionService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void prepararBase() {
    RevisionTestDatos.limpiar(jdbcTemplate);
  }

  @AfterEach
  void limpiarBase() {
    RevisionTestDatos.limpiar(jdbcTemplate);
  }

  @Test
  @DisplayName("Caso 1 - Commit: una revisión válida marca revisado=true y crea exactamente una fila")
  void confirmarRevisionCuandoElPedidoEstaPendiente() {
    // Arrange
    long pedidoId = RevisionTestDatos.insertarPedidoPendiente(jdbcTemplate, "Ana Torres");

    // Act
    RevisionPedidoResultado resultado =
        pedidoRevisionService.revisarPedido(pedidoId, "  Stock y precio verificados  ");

    // Assert
    assertAll(
        () -> assertTrue(RevisionTestDatos.revisado(jdbcTemplate, pedidoId)),
        () -> assertEquals(1, RevisionTestDatos.contarRevisiones(jdbcTemplate, pedidoId)),
        () -> assertEquals("Stock y precio verificados", RevisionTestDatos.observacion(jdbcTemplate, pedidoId)),
        () -> assertEquals(pedidoId, resultado.pedidoId()),
        () -> assertTrue(resultado.revisionId() > 0),
        () -> assertNotNull(resultado.fechaRevision())
    );
  }

  @Test
  @DisplayName("Caso 2 - Revisión duplicada: se rechaza (409) sin crear otra fila ni alterar la primera")
  void rechazarSegundaRevisionDelMismoPedido() {
    // Arrange
    long pedidoId = RevisionTestDatos.insertarPedidoPendiente(jdbcTemplate, "Luis Rojas");
    pedidoRevisionService.revisarPedido(pedidoId, "Primera revisión");

    // Act
    PedidoYaRevisadoException ex = assertThrows(PedidoYaRevisadoException.class,
        () -> pedidoRevisionService.revisarPedido(pedidoId, "Segunda revisión"));

    // Assert
    assertAll(
        () -> assertTrue(ex.getMessage().contains("ya fue revisado")),
        () -> assertEquals(1, RevisionTestDatos.contarRevisiones(jdbcTemplate, pedidoId)),
        () -> assertEquals("Primera revisión", RevisionTestDatos.observacion(jdbcTemplate, pedidoId)),
        () -> assertTrue(RevisionTestDatos.revisado(jdbcTemplate, pedidoId))
    );
  }

  @Test
  @DisplayName("Caso 3 - ID no registrado: respuesta controlada (404) y ninguna escritura")
  void rechazarPedidoInexistenteSinEscrituras() {
    // Arrange
    long idInexistente = 999_999L;

    // Act
    assertThrows(PedidoNoEncontradoException.class,
        () -> pedidoRevisionService.revisarPedido(idInexistente, "Observación válida"));

    // Assert
    assertEquals(0, RevisionTestDatos.contarTodasLasRevisiones(jdbcTemplate));
  }

  @ParameterizedTest(name = "pedidoId = {0}")
  @ValueSource(longs = {0L, -1L, -50L})
  @DisplayName("Caso 3 - pedidoId <= 0: validación (400) antes de tocar la base de datos")
  void rechazarPedidoIdInvalido(long pedidoIdInvalido) {
    // Arrange
    long pedidoReal = RevisionTestDatos.insertarPedidoPendiente(jdbcTemplate, "Rosa Díaz");

    // Act
    assertThrows(RevisionInvalidaException.class,
        () -> pedidoRevisionService.revisarPedido(pedidoIdInvalido, "Observación válida"));

    // Assert
    assertFalse(RevisionTestDatos.revisado(jdbcTemplate, pedidoReal));
    assertEquals(0, RevisionTestDatos.contarTodasLasRevisiones(jdbcTemplate));
  }

  @ParameterizedTest(name = "observacion = [{0}]")
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  @DisplayName("Caso 3 - Observación vacía: validación (400) sin escrituras parciales")
  void rechazarObservacionVacia(String observacion) {
    // Arrange
    long pedidoId = RevisionTestDatos.insertarPedidoPendiente(jdbcTemplate, "Rosa Díaz");

    // Act
    assertThrows(RevisionInvalidaException.class,
        () -> pedidoRevisionService.revisarPedido(pedidoId, observacion));

    // Assert
    assertFalse(RevisionTestDatos.revisado(jdbcTemplate, pedidoId));
    assertEquals(0, RevisionTestDatos.contarRevisiones(jdbcTemplate, pedidoId));
  }

  @Test
  @DisplayName("Rollback por restricción UNIQUE: si el INSERT falla, el UPDATE de revisado se revierte")
  void revertirUpdateCuandoElInsertViolaRestriccionUnica() {
    // Arrange: dato inconsistente provocado a propósito -> pedido pendiente con una fila previa
    long pedidoId = RevisionTestDatos.insertarPedidoPendiente(jdbcTemplate, "Carmen Paz");
    jdbcTemplate.update(
        "INSERT INTO pedido_revision (pedido_id, observacion, fecha_revision) VALUES (?, ?, ?)",
        pedidoId, "Fila previa inconsistente", Timestamp.valueOf(LocalDateTime.now()));

    // Act: UPDATE afecta 1 fila; luego el INSERT viola uk_pedido_revision_pedido
    assertThrows(PedidoYaRevisadoException.class,
        () -> pedidoRevisionService.revisarPedido(pedidoId, "Intento que debe revertirse"));

    // Assert: el UPDATE previo se revirtió y no se agregó otra revisión
    assertFalse(RevisionTestDatos.revisado(jdbcTemplate, pedidoId));
    assertEquals(1, RevisionTestDatos.contarRevisiones(jdbcTemplate, pedidoId));
    assertEquals("Fila previa inconsistente", RevisionTestDatos.observacion(jdbcTemplate, pedidoId));
  }
}
