# pedidos-reportes-service

Servicio REST de consultas y reportes del Sistema de Pedidos (puerto **8083**, contexto `/pedidos-reportes-service`). Lee la base MySQL `pedidosdb` con Spring JDBC.

La documentación completa de PA4 (orden de arranque, variables, rutas, JSON de ejemplo, pruebas y limitaciones) está en `../../Sesión 5/sistema-pedidos-springboot/README.md`. Resumen de lo que corresponde a este servicio:

### E. Rutas nuevas

| Método y ruta | Respuesta |
|---|---|
| `GET /sistema-pedidos/supervision/pedidos?estado=TODOS\|PENDIENTE\|REVISADO` | JSP; **503** con aviso si 8083 no responde |
| `POST /sistema-pedidos/supervision/pedidos/{id}/revisar` (`observacion`, `_csrf`) | **302** → GET (PRG) · **400** inválido · **404** no existe · **409** ya revisado |
| `GET /pedidos-reportes-service/api/v1/pedidos/revisiones?estado=PENDIENTE\|REVISADO` | **200** JSON · **400** ProblemDetail si el estado no es válido |
| `GET /pedidos-reportes-service/api/v1/pedidos/estadisticas/revisiones` | **200** JSON |

**JSON de ejemplo**

```json
GET /api/v1/pedidos/revisiones?estado=REVISADO
[{"id":1,"cliente":"Ana Torres","productoCodigo":"LAP-001","productoNombre":"Laptop",
  "cantidad":2,"total":3000.00,"fecha":"2026-11-20T10:00:00","estadoRevision":"REVISADO",
  "revisado":true,"observacion":"Stock y precio verificados","fechaRevision":"2026-11-21T15:45:00"}]

GET /api/v1/pedidos/estadisticas/revisiones
{"totalPedidos":3,"totalRevisados":1,"totalPendientes":2,"porcentajeRevisado":33.33}

GET /api/v1/pedidos/revisiones?estado=INVALIDO   (400, application/problem+json)
{"type":"about:blank","title":"Parámetro inválido","status":400,
 "detail":"Estado de revisión no válido: 'INVALIDO'. Valores permitidos: PENDIENTE, REVISADO.",
 "instance":"/pedidos-reportes-service/api/v1/pedidos/revisiones"}
```

Colección Postman: `postman/PA4-revision.postman_collection.json`.

### Pruebas

```bash
./mvnw test   # H2 en memoria (src/test/resources), no requiere MySQL
```

- `PedidoRevisionReporteControllerIntegrationTest`: contrato REST 200/400 y JSON.
- `PedidoRevisionReporteServiceImplTest`: cálculo de porcentaje y conversión de estado.

Permiso MySQL requerido: `GRANT SELECT ON pedidosdb.pedido_revision TO 'spedidos_reportes'@'localhost';`
