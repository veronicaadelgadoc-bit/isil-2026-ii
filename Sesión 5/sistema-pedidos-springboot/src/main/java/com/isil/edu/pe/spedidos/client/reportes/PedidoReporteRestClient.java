package com.isil.edu.pe.spedidos.client.reportes;

import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoReporteResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionReporteResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.ProductoVendidoResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.ResumenPedidosResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PedidoReporteRestClient implements PedidoReporteClient {

  private final RestClient restClient;
  private final String baseUrl;

  public PedidoReporteRestClient(
      @Qualifier("pedidoReporteHttpClient") RestClient restClient,
      @Value("${pedido-reportes.api.base-url}") String baseUrl) {
    this.restClient = restClient;
    this.baseUrl = baseUrl;
  }

  @Override
  public ResumenPedidosResponse obtenerResumen() {
    try {
      ResumenPedidosResponse response = restClient.get()
          .uri(baseUrl + "/estadisticas/resumen")
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(ResumenPedidosResponse.class);
      if (response == null) {
        throw new PedidoReporteNoDisponibleException("El servicio de reportes devolvió una respuesta vacía", null);
      }
      return response;
    } catch (RestClientResponseException | ResourceAccessException e) {
      throw noDisponible(e);
    }
  }

  @Override
  public List<PedidoReporteResponse> buscarPorCliente(String cliente) {
    if (cliente == null || cliente.isBlank()) {
      return List.of();
    }
    try {
      List<PedidoReporteResponse> response = restClient.get()
          .uri(baseUrl + "?cliente={cliente}", cliente.trim())
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(new ParameterizedTypeReference<List<PedidoReporteResponse>>() { });
      return response == null ? List.of() : response;
    } catch (RestClientResponseException | ResourceAccessException e) {
      throw noDisponible(e);
    }
  }

  @Override
  public List<ProductoVendidoResponse> listarProductosMasVendidos() {
    try {
      List<ProductoVendidoResponse> response = restClient.get()
          .uri(baseUrl + "/estadisticas/productos")
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(new ParameterizedTypeReference<List<ProductoVendidoResponse>>() { });
      return response == null ? List.of() : response;
    } catch (RestClientResponseException | ResourceAccessException e) {
      throw noDisponible(e);
    }
  }

  private PedidoReporteNoDisponibleException noDisponible(Exception e) {
    return new PedidoReporteNoDisponibleException(
        "pedidos-reportes-service no está disponible. Verifique que se ejecute en el puerto 8083.", e);
  }

  // ===================== PA4 - Revisión de pedidos =====================

  @Override
  public List<PedidoRevisionReporteResponse> listarRevisiones(EstadoRevision estado) {
    try {
      RestClient.RequestHeadersSpec<?> request = (estado == null)
          ? restClient.get().uri(baseUrl + "/revisiones")
          : restClient.get().uri(baseUrl + "/revisiones?estado={estado}", estado.name());

      List<PedidoRevisionReporteResponse> response = request
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(new ParameterizedTypeReference<List<PedidoRevisionReporteResponse>>() { });
      return response == null ? List.of() : response;
    } catch (RestClientResponseException e) {
      throw traducirRespuestaError(e);
    } catch (RestClientException e) {
      // Timeout, conexión rechazada o respuesta ilegible: el detalle técnico queda en la causa.
      throw noDisponible(e);
    }
  }

  @Override
  public EstadisticaRevisionResponse obtenerEstadisticasRevision() {
    try {
      EstadisticaRevisionResponse response = restClient.get()
          .uri(baseUrl + "/estadisticas/revisiones")
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(EstadisticaRevisionResponse.class);
      if (response == null) {
        throw new PedidoReporteNoDisponibleException(
            "El servicio de reportes devolvió una respuesta vacía.", null);
      }
      return response;
    } catch (RestClientResponseException e) {
      throw traducirRespuestaError(e);
    } catch (RestClientException e) {
      throw noDisponible(e);
    }
  }

  /**
   * 5xx -> servicio no disponible; 4xx -> solicitud inválida. Nunca se propaga la traza al usuario.
   */
  private RuntimeException traducirRespuestaError(RestClientResponseException e) {
    int status = e.getStatusCode().value();
    if (status >= 500) {
      return new PedidoReporteNoDisponibleException(
          "pedidos-reportes-service respondió con un error temporal (HTTP " + status + ").", e);
    }
    return new PedidoReporteSolicitudInvalidaException(
        "pedidos-reportes-service rechazó la consulta (HTTP " + status + ").", status, e);
  }
}
