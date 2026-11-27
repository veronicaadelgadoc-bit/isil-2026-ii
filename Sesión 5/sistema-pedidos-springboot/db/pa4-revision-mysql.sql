-- =====================================================================
-- PA4 - Módulo de revisión y trazabilidad de pedidos (MySQL 8)
-- Base: pedidosdb
--
-- Opción A (por defecto): no ejecutar este script; Hibernate
--   (spring.jpa.hibernate.ddl-auto=update) agrega la columna y la tabla al
--   iniciar sistema-pedidos-springboot.
-- Opción B: ejecutar las secciones 1 y 2 ANTES del primer arranque.
-- La sección 3 (permisos) es necesaria en ambos casos.
-- =====================================================================
USE pedidosdb;

-- 1. Atributo revisado (valor inicial FALSE para pedidos existentes y nuevos)
ALTER TABLE pedido
    ADD COLUMN revisado BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Trazabilidad: una sola revisión confirmada por pedido
CREATE TABLE IF NOT EXISTS pedido_revision (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    pedido_id      BIGINT       NOT NULL,
    observacion    VARCHAR(500) NOT NULL,
    fecha_revision DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_pedido_revision_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_pedido_revision_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id)
) ENGINE = InnoDB;

-- 3. Permisos de laboratorio (ajustar host/usuario a su entorno)
GRANT SELECT, INSERT, UPDATE, DELETE ON pedidosdb.pedido_revision TO 'spedidos_app'@'localhost';
GRANT SELECT ON pedidosdb.pedido_revision TO 'spedidos_reportes'@'localhost';
FLUSH PRIVILEGES;

-- =====================================================================
-- Consultas de evidencia (antes / después de cada caso)
-- =====================================================================
-- Estado de un pedido y su revisión (reemplazar 15 por el ID probado):
-- SELECT p.id, p.cliente, p.total, p.revisado, r.id AS revision_id, r.observacion, r.fecha_revision
-- FROM pedido p LEFT JOIN pedido_revision r ON r.pedido_id = p.id
-- WHERE p.id = 15;
--
-- Cantidad de revisiones por pedido (nunca debe superar 1):
-- SELECT pedido_id, COUNT(*) AS revisiones FROM pedido_revision GROUP BY pedido_id;
--
-- Restricciones creadas:
-- SELECT constraint_name, constraint_type FROM information_schema.table_constraints
-- WHERE table_schema = 'pedidosdb' AND table_name = 'pedido_revision';
