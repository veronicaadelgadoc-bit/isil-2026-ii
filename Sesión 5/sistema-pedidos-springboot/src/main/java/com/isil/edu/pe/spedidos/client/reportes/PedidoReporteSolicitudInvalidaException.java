package com.isil.edu.pe.spedidos.client.reportes;

/**
 * PA4 - pedidos-reportes-service rechazó la solicitud (HTTP 4xx).
 * Se diferencia de {@link PedidoReporteNoDisponibleException} (timeout, conexión, 5xx).
 */
public class PedidoReporteSolicitudInvalidaException extends RuntimeException {

  private final int status;

  public PedidoReporteSolicitudInvalidaException(String message, int status, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
