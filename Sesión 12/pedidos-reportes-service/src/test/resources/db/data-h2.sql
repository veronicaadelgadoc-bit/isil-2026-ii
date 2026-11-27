-- PA4 - Datos de prueba: 3 pedidos, 1 revisado (33.33 %).
INSERT INTO pedido (id, cliente, producto_codigo, producto_nombre, precio_unitario, cantidad, total, fecha, revisado)
VALUES (1, 'Ana Torres', 'LAP-001', 'Laptop', 1500.00, 2, 3000.00, TIMESTAMP '2026-11-20 10:00:00', TRUE);
INSERT INTO pedido (id, cliente, producto_codigo, producto_nombre, precio_unitario, cantidad, total, fecha, revisado)
VALUES (2, 'Luis Rojas', 'MOU-010', 'Mouse', 50.00, 3, 150.00, TIMESTAMP '2026-11-20 11:00:00', FALSE);
INSERT INTO pedido (id, cliente, producto_codigo, producto_nombre, precio_unitario, cantidad, total, fecha, revisado)
VALUES (3, 'Rosa Díaz', 'TEC-020', 'Teclado', 120.00, 1, 120.00, TIMESTAMP '2026-11-21 09:30:00', FALSE);

INSERT INTO pedido_revision (pedido_id, observacion, fecha_revision)
VALUES (1, 'Stock y precio verificados', TIMESTAMP '2026-11-21 15:45:00');
