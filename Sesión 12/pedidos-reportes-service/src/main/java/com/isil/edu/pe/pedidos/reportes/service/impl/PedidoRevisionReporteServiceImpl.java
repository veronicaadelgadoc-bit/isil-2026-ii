package com.isil.edu.pe.pedidos.reportes.service.impl;

import com.isil.edu.pe.pedidos.reportes.exception.EstadoRevisionInvalidoException;
import com.isil.edu.pe.pedidos.reportes.repository.PedidoRevisionReporteJdbcRepository;
import com.isil.edu.pe.pedidos.reportes.repository.projection.ConteoRevisionProjection;
import com.isil.edu.pe.pedidos.reportes.service.PedidoRevisionReporteService;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadisticaRevision;
import com.isil.edu.pe.pedidos.reportes.service.domain.EstadoRevision;
import com.isil.edu.pe.pedidos.reportes.service.domain.PedidoRevisionEstado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PA4 - Reglas de consulta y cálculo de estadísticas de revisión.
 */
@Service
@Transactional(readOnly = true)
public class PedidoRevisionReporteServiceImpl implements PedidoRevisionReporteService {

  private static final BigDecimal CIEN = BigDecimal.valueOf(100);

  private final PedidoRevisionReporteJdbcRepository repository;

  public PedidoRevisionReporteServiceImpl(PedidoRevisionReporteJdbcRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<PedidoRevisionEstado> listarPorEstado(String estado) {
    if (estado == null || estado.isBlank()) {
      return repository.listarTodos();
    }
    EstadoRevision filtro = convertir(estado);
    return repository.listarPorEstado(filtro == EstadoRevision.REVISADO);
  }

  @Override
  public EstadisticaRevision obtenerEstadisticas() {
    ConteoRevisionProjection conteo = repository.contarRevisiones();
    long total = conteo.totalPedidos();
    long revisados = conteo.totalRevisados();
    long pendientes = total - revisados;

    BigDecimal porcentaje = total == 0
        ? BigDecimal.ZERO.setScale(2)
        : BigDecimal.valueOf(revisados).multiply(CIEN).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

    return new EstadisticaRevision(total, revisados, pendientes, porcentaje);
  }

  private EstadoRevision convertir(String estado) {
    try {
      return EstadoRevision.valueOf(estado.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new EstadoRevisionInvalidoException(estado);
    }
  }
}
