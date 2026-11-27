package com.isil.edu.pe.spedidos.client.reportes;

import com.isil.edu.pe.spedidos.client.reportes.dto.EstadisticaRevisionResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoReporteResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.PedidoRevisionReporteResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.ProductoVendidoResponse;
import com.isil.edu.pe.spedidos.client.reportes.dto.ResumenPedidosResponse;
import java.util.List;

public interface PedidoReporteClient {

  ResumenPedidosResponse obtenerResumen();

  List<PedidoReporteResponse> buscarPorCliente(String cliente);

  List<ProductoVendidoResponse> listarProductosMasVendidos();


  // ===================== PA4 - Revisión de pedidos =====================

  /**
   * PA4 - Consume {@code GET /api/v1/pedidos/revisiones?estado=}.
   *
   * @param estado filtro; {@code null} para obtener todos los pedidos
   * @throws PedidoReporteNoDisponibleException ante timeout, error de conexión o 5xx
   * @throws PedidoReporteSolicitudInvalidaException ante una respuesta 4xx
   */
  List<PedidoRevisionReporteResponse> listarRevisiones(EstadoRevision estado);

  /**
   * PA4 - Consume {@code GET /api/v1/pedidos/estadisticas/revisiones}.
   *
   * @throws PedidoReporteNoDisponibleException ante timeout, error de conexión o 5xx
   */
  EstadisticaRevisionResponse obtenerEstadisticasRevision();
}
