package com.isil.edu.pe.spedidos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * PA4 - Mapeo de la tabla {@code pedido_revision} (trazabilidad de la revisión).
 *
 * <p>Esta clase existe <strong>solo para declarar el esquema</strong> (DDL) de forma
 * portable entre MySQL y H2: clave primaria, integridad referencial hacia
 * {@code pedido} y la restricción única que impide más de una revisión confirmada
 * por pedido. Las escrituras y lecturas del flujo de revisión NO usan JPA: se
 * realizan con SQL parametrizado en {@code PedidoRevisionJdbcRepository}.</p>
 *
 * <pre>
 * pedido (1) ──────── (0..1) pedido_revision
 *   id  PK                    id             PK
 *   revisado BOOLEAN          pedido_id      FK -> pedido.id, UNIQUE
 *                             observacion    VARCHAR(500) NOT NULL
 *                             fecha_revision DATETIME NOT NULL
 * </pre>
 */
@Entity
@Table(
    name = "pedido_revision",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pedido_revision_pedido",
        columnNames = "pedido_id"
    )
)
public class PedidoRevision {

  public static final int OBSERVACION_MAX = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "pedido_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_pedido_revision_pedido")
  )
  private Pedido pedido;

  @Column(nullable = false, length = OBSERVACION_MAX)
  private String observacion;

  @Column(name = "fecha_revision", nullable = false)
  private LocalDateTime fechaRevision;

  protected PedidoRevision() {
    // Requerido por JPA. No se instancia desde el flujo de revisión (usa JDBC).
  }

  public Long getId() {
    return id;
  }

  public Pedido getPedido() {
    return pedido;
  }

  public String getObservacion() {
    return observacion;
  }

  public LocalDateTime getFechaRevision() {
    return fechaRevision;
  }
}
