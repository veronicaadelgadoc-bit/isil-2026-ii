package com.isil.edu.pe.pedidos.reportes.service;

import com.isil.edu.pe.pedidos.reportes.exception.EstadoRevisionInvalidoException;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadisticaRevision;
import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevisionEstado;
import java.util.List;

/**
 * PA4 - Consultas del estado de revisión de pedidos.
 */
public interface PedidoRevisionReporteService {

  /**
   * @param estado PENDIENTE, REVISADO (sin distinguir mayúsculas) o vacío para todos
   * @throws EstadoRevisionInvalidoException si el estado no es reconocido
   */
  List<PedidoRevisionEstado> listarPorEstado(String estado);

  EstadisticaRevision obtenerEstadisticas();
}
