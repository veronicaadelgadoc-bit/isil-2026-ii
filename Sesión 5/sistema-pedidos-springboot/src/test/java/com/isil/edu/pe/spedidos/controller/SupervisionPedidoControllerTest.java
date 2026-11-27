package com.isil.edu.pe.spedidos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.isil.edu.pe.spedidos.client.reportes.EstadoRevision;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteClient;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteNoDisponibleException;
import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionReporteResponse;
import com.isil.edu.pe.spedidos.exception.PedidoNoEncontradoException;
import com.isil.edu.pe.spedidos.exception.PedidoYaRevisadoException;
import com.isil.edu.pe.spedidos.exception.RevisionInvalidaException;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import com.isil.edu.pe.spedidos.service.dto.RevisionPedidoResultado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * PA4 - Integración Spring MVC + Spring Security de la supervisión.
 *
 * <p>Pasa por la cadena real: filtros de seguridad (roles y CSRF) → DispatcherServlet →
 * {@link SupervisionPedidoController}. El Service y el cliente HTTP se aíslan con Mockito.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SupervisionPedidoControllerTest {

  @Autowired
  private WebApplicationContext context;

  @MockitoBean
  private PedidoRevisionService pedidoRevisionService;

  @MockitoBean
  private PedidoReporteClient pedidoReporteClient;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private void servicioReportesResponde() {
    PedidoRevisionReporteResponse pendiente = new PedidoRevisionReporteResponse(
        10L, "Ana Torres", "LAP-001", "Laptop", 2, new BigDecimal("3000.00"),
        LocalDateTime.of(2026, 11, 20, 10, 0), "PENDIENTE", false, null, null);
    when(pedidoReporteClient.listarRevisiones(any())).thenReturn(List.of(pendiente));
    when(pedidoReporteClient.obtenerEstadisticasRevision())
        .thenReturn(new EstadisticaRevisionResponse(1, 0, 1, new BigDecimal("0.00")));
  }

  @Test
  @DisplayName("GET /supervision/pedidos arma el Model con datos obtenidos vía cliente HTTP")
  void mostrarSupervisionConDatosDelServicioDeReportes() throws Exception {
    // Arrange
    servicioReportesResponde();

    // Act / Assert
    mockMvc.perform(get("/supervision/pedidos").param("estado", "pendiente")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(view().name("supervision-pedidos"))
        .andExpect(model().attribute("estado", "PENDIENTE"))
        .andExpect(model().attribute("servicioReportesDisponible", true))
        .andExpect(model().attributeExists("pedidos", "estadisticas"));
    verify(pedidoReporteClient).listarRevisiones(EstadoRevision.PENDIENTE);
  }

  @Test
  @DisplayName("Caso 5 - Con reportes caído la vista responde 503 controlado y no escribe en BD")
  void mostrarAvisoCuandoServicioDeReportesNoEstaDisponible() throws Exception {
    // Arrange
    when(pedidoReporteClient.listarRevisiones(any()))
        .thenThrow(new PedidoReporteNoDisponibleException("pedidos-reportes-service no está disponible.", null));

    // Act / Assert
    mockMvc.perform(get("/supervision/pedidos").with(user("operador").roles("OPERADOR")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(view().name("supervision-pedidos"))
        .andExpect(model().attribute("servicioReportesDisponible", false))
        .andExpect(model().attributeExists("errorServicio"));
    verifyNoInteractions(pedidoRevisionService);
  }

  @Test
  @DisplayName("POST revisar válido delega al Service y aplica Post/Redirect/Get")
  void revisarPedidoYRedirigirCuandoEsValido() throws Exception {
    // Arrange
    when(pedidoRevisionService.revisarPedido(15L, "Conforme"))
        .thenReturn(new RevisionPedidoResultado(1L, 15L, "Conforme", LocalDateTime.now()));

    // Act / Assert
    mockMvc.perform(post("/supervision/pedidos/15/revisar")
            .param("observacion", "Conforme")
            .with(user("operador").roles("OPERADOR"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/supervision/pedidos"))
        .andExpect(flash().attributeExists("mensaje"));
    verify(pedidoRevisionService).revisarPedido(15L, "Conforme");
  }

  @Test
  @DisplayName("Caso 2 - POST sobre pedido ya revisado responde 409 con mensaje")
  void responderConflictoCuandoElPedidoYaFueRevisado() throws Exception {
    // Arrange
    servicioReportesResponde();
    when(pedidoRevisionService.revisarPedido(eq(15L), anyString()))
        .thenThrow(new PedidoYaRevisadoException(15L));

    // Act / Assert
    mockMvc.perform(post("/supervision/pedidos/15/revisar")
            .param("observacion", "Otra vez")
            .with(user("operador").roles("OPERADOR"))
            .with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(view().name("supervision-pedidos"))
        .andExpect(model().attributeExists("error"));
  }

  @Test
  @DisplayName("Caso 3 - POST con ID no registrado responde 404")
  void responderNoEncontradoCuandoElPedidoNoExiste() throws Exception {
    // Arrange
    servicioReportesResponde();
    when(pedidoRevisionService.revisarPedido(eq(999L), anyString()))
        .thenThrow(new PedidoNoEncontradoException(999L));

    // Act / Assert
    mockMvc.perform(post("/supervision/pedidos/999/revisar")
            .param("observacion", "Revisión")
            .with(user("operador").roles("OPERADOR"))
            .with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(model().attributeExists("error"));
  }

  @Test
  @DisplayName("Caso 3 - POST con observación vacía responde 400")
  void responderBadRequestCuandoLaObservacionEstaVacia() throws Exception {
    // Arrange
    servicioReportesResponde();
    when(pedidoRevisionService.revisarPedido(15L, ""))
        .thenThrow(new RevisionInvalidaException("La observación de la revisión es obligatoria."));

    // Act / Assert
    mockMvc.perform(post("/supervision/pedidos/15/revisar")
            .param("observacion", "")
            .with(user("operador").roles("OPERADOR"))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(model().attribute("error", "La observación de la revisión es obligatoria."));
  }

  @Test
  @DisplayName("Caso 3 - POST con ID no numérico responde 400 sin invocar al Service")
  void responderBadRequestCuandoElIdNoEsNumerico() throws Exception {
    // Arrange
    servicioReportesResponde();

    // Act / Assert
    mockMvc.perform(post("/supervision/pedidos/abc/revisar")
            .param("observacion", "Revisión")
            .with(user("operador").roles("OPERADOR"))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(model().attributeExists("error"));
    verifyNoInteractions(pedidoRevisionService);
  }

  @Test
  @DisplayName("Seguridad - POST sin token CSRF es rechazado (403) antes del Controller")
  void rechazarRevisionSinCsrf() throws Exception {
    mockMvc.perform(post("/supervision/pedidos/15/revisar")
            .param("observacion", "Conforme")
            .with(user("operador").roles("OPERADOR")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(pedidoRevisionService);
  }

  @Test
  @DisplayName("Seguridad - El rol CONSULTA no puede revisar pedidos (403)")
  void rechazarRevisionConRolConsulta() throws Exception {
    mockMvc.perform(post("/supervision/pedidos/15/revisar")
            .param("observacion", "Conforme")
            .with(user("consulta").roles("CONSULTA"))
            .with(csrf()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(pedidoRevisionService);
  }
}
