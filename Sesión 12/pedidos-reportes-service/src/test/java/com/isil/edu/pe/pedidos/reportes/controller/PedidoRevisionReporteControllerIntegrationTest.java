package com.isil.edu.pe.pedidos.reportes.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * PA4 - Contrato REST de los recursos nuevos (Controller -> Service -> Repository JDBC -> H2).
 *
 * <p>Datos: 3 pedidos (src/test/resources/db/data-h2.sql), solo el ID 1 revisado.</p>
 */
@SpringBootTest
class PedidoRevisionReporteControllerIntegrationTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  @DisplayName("GET /revisiones?estado=REVISADO devuelve 200 JSON con observación y fecha de revisión")
  void listarRevisadosConObservacionYFecha() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "REVISADO")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].estadoRevision").value("REVISADO"))
        .andExpect(jsonPath("$[0].revisado").value(true))
        .andExpect(jsonPath("$[0].observacion").value("Stock y precio verificados"))
        .andExpect(jsonPath("$[0].fechaRevision").exists());
  }

  @Test
  @DisplayName("GET /revisiones?estado=PENDIENTE devuelve solo pendientes y sin observación")
  void listarPendientesSinObservacion() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "pendiente"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].revisado", everyItem(is(false))))
        .andExpect(jsonPath("$[*].estadoRevision", everyItem(is("PENDIENTE"))))
        .andExpect(jsonPath("$[0].observacion").value(nullValue()));
  }

  @Test
  @DisplayName("GET /revisiones sin estado devuelve todos los pedidos ordenados por ID descendente")
  void listarTodosCuandoNoSeEnviaEstado() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/revisiones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].id").value(3));
  }

  @Test
  @DisplayName("GET /revisiones?estado=INVALIDO responde 400 con ProblemDetail")
  void responderBadRequestCuandoElEstadoEsInvalido() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/revisiones").param("estado", "INVALIDO"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Parámetro inválido"));
  }

  @Test
  @DisplayName("GET /estadisticas/revisiones devuelve totales y porcentaje calculados en Service")
  void obtenerEstadisticasDeRevision() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/estadisticas/revisiones"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPedidos").value(3))
        .andExpect(jsonPath("$.totalRevisados").value(1))
        .andExpect(jsonPath("$.totalPendientes").value(2))
        .andExpect(jsonPath("$.porcentajeRevisado").value(33.33));
  }
}
