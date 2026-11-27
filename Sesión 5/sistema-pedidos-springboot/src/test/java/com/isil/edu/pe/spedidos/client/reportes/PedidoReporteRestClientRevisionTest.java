package com.isil.edu.pe.spedidos.client.reportes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionReporteResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * PA4 - Contrato y manejo de indisponibilidad del cliente HTTP hacia pedidos-reportes-service.
 *
 * <p>Prueba unitaria sin contexto Spring: {@link MockRestServiceServer} simula el
 * servicio remoto; para la caída real se usa un puerto sin proceso escuchando.</p>
 */
class PedidoReporteRestClientRevisionTest {

  private static final String BASE_URL = "http://localhost:8083/pedidos-reportes-service/api/v1/pedidos";

  private MockRestServiceServer servidor;
  private PedidoReporteRestClient cliente;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    servidor = MockRestServiceServer.bindTo(builder).build();
    cliente = new PedidoReporteRestClient(builder.build(), BASE_URL);
  }

  @Test
  @DisplayName("Mapea el JSON de /revisiones?estado=REVISADO al DTO de transporte")
  void mapearListadoDeRevisionesCuandoElServicioRespondeOk() {
    // Arrange
    String json = """
        [{"id":7,"cliente":"Ana Torres","productoCodigo":"LAP-001","productoNombre":"Laptop",
          "cantidad":2,"total":3000.00,"fecha":"2026-11-20T10:15:30",
          "estadoRevision":"REVISADO","revisado":true,
          "observacion":"Stock verificado","fechaRevision":"2026-11-21T09:00:00"}]
        """;
    servidor.expect(requestTo(BASE_URL + "/revisiones?estado=REVISADO"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    // Act
    List<PedidoRevisionReporteResponse> resultado = cliente.listarRevisiones(EstadoRevision.REVISADO);

    // Assert
    servidor.verify();
    assertEquals(1, resultado.size());
    PedidoRevisionReporteResponse pedido = resultado.get(0);
    assertAll(
        () -> assertEquals(7L, pedido.id()),
        () -> assertTrue(pedido.revisado()),
        () -> assertEquals("REVISADO", pedido.estadoRevision()),
        () -> assertEquals("Stock verificado", pedido.observacion()),
        () -> assertEquals(0, new BigDecimal("3000.00").compareTo(pedido.total())),
        () -> assertEquals(2026, pedido.fechaRevision().getYear())
    );
  }

  @Test
  @DisplayName("Sin filtro consume /revisiones sin parámetro estado")
  void consumirRevisionesSinFiltroCuandoEstadoEsNulo() {
    // Arrange
    servidor.expect(requestTo(BASE_URL + "/revisiones"))
        .andRespond(withSuccess("""
            [{"id":3,"cliente":"Luis","productoCodigo":"P1","productoNombre":"Mouse","cantidad":1,
              "total":50.00,"fecha":"2026-11-20T10:00:00","estadoRevision":"PENDIENTE",
              "revisado":false,"observacion":null,"fechaRevision":null}]
            """, MediaType.APPLICATION_JSON));

    // Act
    List<PedidoRevisionReporteResponse> resultado = cliente.listarRevisiones(null);

    // Assert
    servidor.verify();
    assertFalse(resultado.get(0).revisado());
    assertNull(resultado.get(0).observacion());
  }

  @Test
  @DisplayName("Mapea el resumen /estadisticas/revisiones")
  void mapearEstadisticasDeRevision() {
    // Arrange
    servidor.expect(requestTo(BASE_URL + "/estadisticas/revisiones"))
        .andRespond(withSuccess("""
            {"totalPedidos":4,"totalRevisados":1,"totalPendientes":3,"porcentajeRevisado":25.00}
            """, MediaType.APPLICATION_JSON));

    // Act
    EstadisticaRevisionResponse resumen = cliente.obtenerEstadisticasRevision();

    // Assert
    assertAll(
        () -> assertEquals(4, resumen.totalPedidos()),
        () -> assertEquals(1, resumen.totalRevisados()),
        () -> assertEquals(3, resumen.totalPendientes()),
        () -> assertEquals(0, new BigDecimal("25.00").compareTo(resumen.porcentajeRevisado()))
    );
  }

  @Test
  @DisplayName("HTTP 503 del servicio remoto se traduce a PedidoReporteNoDisponibleException controlada")
  void traducirError503AExcepcionControlada() {
    // Arrange
    servidor.expect(requestTo(BASE_URL + "/estadisticas/revisiones"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    // Act
    PedidoReporteNoDisponibleException ex = assertThrows(PedidoReporteNoDisponibleException.class,
        () -> cliente.obtenerEstadisticasRevision());

    // Assert: mensaje comprensible, sin nombres de clases ni trazas
    assertTrue(ex.getMessage().contains("HTTP 503"));
    assertFalse(ex.getMessage().contains("Exception"));
  }

  @Test
  @DisplayName("HTTP 500 en el listado también se considera indisponibilidad")
  void traducirError500AExcepcionControlada() {
    // Arrange
    servidor.expect(requestTo(BASE_URL + "/revisiones?estado=PENDIENTE"))
        .andRespond(withServerError());

    // Act / Assert
    assertThrows(PedidoReporteNoDisponibleException.class,
        () -> cliente.listarRevisiones(EstadoRevision.PENDIENTE));
  }

  @Test
  @DisplayName("HTTP 400 del servicio remoto se traduce a solicitud inválida con su código")
  void traducirError400ASolicitudInvalida() {
    // Arrange
    servidor.expect(requestTo(BASE_URL + "/revisiones?estado=REVISADO"))
        .andRespond(withBadRequest());

    // Act
    PedidoReporteSolicitudInvalidaException ex = assertThrows(PedidoReporteSolicitudInvalidaException.class,
        () -> cliente.listarRevisiones(EstadoRevision.REVISADO));

    // Assert
    assertEquals(400, ex.getStatus());
  }

  @Test
  @DisplayName("Caso 5 - Servicio detenido (conexión rechazada) se traduce a respuesta controlada")
  void traducirConexionRechazadaAExcepcionControlada() {
    // Arrange: puerto 1 sin proceso escuchando -> ConnectException
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(500));
    factory.setReadTimeout(Duration.ofMillis(500));
    PedidoReporteRestClient clienteCaido = new PedidoReporteRestClient(
        RestClient.builder().requestFactory(factory).build(),
        "http://127.0.0.1:1/pedidos-reportes-service/api/v1/pedidos");

    // Act
    PedidoReporteNoDisponibleException ex = assertThrows(PedidoReporteNoDisponibleException.class,
        () -> clienteCaido.listarRevisiones(null));

    // Assert
    assertTrue(ex.getMessage().contains("8083"));
  }
}
