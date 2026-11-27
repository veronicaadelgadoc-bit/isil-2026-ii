package com.isil.edu.pe.pedidos.reportes.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.pedidos.reportes.exception.EstadoRevisionInvalidoException;
import com.isil.edu.pe.pedidos.reportes.repository.PedidoRevisionReporteJdbcRepository;
import com.isil.edu.pe.pedidos.reportes.repository.projection.ConteoRevisionProjection;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadisticaRevision;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PA4 - Pruebas unitarias (Mockito) de las reglas de la capa Service.
 */
class PedidoRevisionReporteServiceImplTest {

  private PedidoRevisionReporteJdbcRepository repository;
  private PedidoRevisionReporteServiceImpl service;

  @BeforeEach
  void setUp() {
    repository = mock(PedidoRevisionReporteJdbcRepository.class);
    service = new PedidoRevisionReporteServiceImpl(repository);
  }

  @Test
  @DisplayName("Calcula pendientes y porcentaje con redondeo HALF_UP a 2 decimales")
  void calcularPorcentajeRevisado() {
    // Arrange
    when(repository.contarRevisiones()).thenReturn(new ConteoRevisionProjection(3, 2));

    // Act
    EstadisticaRevision resultado = service.obtenerEstadisticas();

    // Assert
    assertAll(
        () -> assertEquals(3, resultado.totalPedidos()),
        () -> assertEquals(2, resultado.totalRevisados()),
        () -> assertEquals(1, resultado.totalPendientes()),
        () -> assertEquals(new BigDecimal("66.67"), resultado.porcentajeRevisado())
    );
  }

  @Test
  @DisplayName("Sin pedidos el porcentaje es 0.00 (evita división entre cero)")
  void devolverCeroCuandoNoHayPedidos() {
    // Arrange
    when(repository.contarRevisiones()).thenReturn(new ConteoRevisionProjection(0, 0));

    // Act
    EstadisticaRevision resultado = service.obtenerEstadisticas();

    // Assert
    assertEquals(new BigDecimal("0.00"), resultado.porcentajeRevisado());
    assertEquals(0, resultado.totalPendientes());
  }

  @Test
  @DisplayName("Convierte el estado sin distinguir mayúsculas y lo envía como parámetro boolean")
  void filtrarPorEstadoSinDistinguirMayusculas() {
    // Arrange
    when(repository.listarPorEstado(true)).thenReturn(List.of());

    // Act
    service.listarPorEstado("  revisado ");

    // Assert
    verify(repository).listarPorEstado(true);
  }

  @Test
  @DisplayName("Estado inválido lanza excepción de negocio sin consultar la base de datos")
  void rechazarEstadoInvalidoSinConsultarRepositorio() {
    // Act / Assert
    assertThrows(EstadoRevisionInvalidoException.class, () -> service.listarPorEstado("APROBADO"));
    verifyNoInteractions(repository);
  }
}
