-- PA4 - Esquema equivalente al de MySQL (pedidosdb) para pruebas.
DROP TABLE IF EXISTS pedido_revision;
DROP TABLE IF EXISTS pedido;

CREATE TABLE pedido (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente         VARCHAR(120)   NOT NULL,
    producto_codigo VARCHAR(30)    NOT NULL,
    producto_nombre VARCHAR(120)   NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    cantidad        INT            NOT NULL,
    total           DECIMAL(12, 2) NOT NULL,
    fecha           TIMESTAMP      NOT NULL,
    revisado        BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE pedido_revision (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id      BIGINT       NOT NULL,
    observacion    VARCHAR(500) NOT NULL,
    fecha_revision TIMESTAMP    NOT NULL,
    CONSTRAINT uk_pedido_revision_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_pedido_revision_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id)
);
