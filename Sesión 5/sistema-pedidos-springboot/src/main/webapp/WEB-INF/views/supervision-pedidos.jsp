<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  PA4 - Supervisión y revisión de pedidos.
  Solo presenta datos del Model: no contiene SQL ni reglas de negocio.
  Listado/resumen: PedidoReporteClient -> pedidos-reportes-service.
  Revisión: POST /supervision/pedidos/{id}/revisar (CSRF) -> PedidoRevisionService.
--%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Supervisión de Pedidos - ISIL</title>
    <c:url var="cssUrl" value="/assets/css/app.css"/>
    <link rel="stylesheet" href="${cssUrl}">
</head>
<body>
<c:url var="supervisionUrl" value="/supervision/pedidos"/>
<div class="contenedor">
    <header class="cabecera">
        <div>
            <p class="eyebrow">PA4 · Spring MVC + Spring JDBC + REST</p>
            <h1>Supervisión de pedidos</h1>
            <p class="subtitulo">
                Revisión transaccional local (MySQL) y consulta del estado vía pedidos-reportes-service.
            </p>
        </div>

        <div class="acciones-cabecera">
            <c:url var="pedidosUrl" value="/pedidos"/>
            <a href="${pedidosUrl}" class="boton-link boton-link-secundario">
                <span aria-hidden="true">←</span>
                Volver a pedidos
            </a>
            <c:url var="logoutUrl" value="/logout"/>
            <form method="post" action="${logoutUrl}">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button type="submit">Cerrar sesión</button>
            </form>
        </div>
    </header>

    <c:if test="${not empty mensaje}">
        <div class="alerta exito" role="status"><c:out value="${mensaje}"/></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alerta error" role="alert"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${not servicioReportesDisponible}">
        <div class="alerta advertencia" role="alert">
            <strong>Servicio de reportes no disponible.</strong>
            <c:out value="${errorServicio}"/>
            La revisión local sigue operativa mediante el formulario "Revisar por ID".
        </div>
    </c:if>

    <section class="tarjeta">
        <h2>Resumen de revisiones</h2>
        <div class="kpis">
            <div class="kpi"><span>Total pedidos</span>
                <strong><c:out value="${empty estadisticas ? '—' : estadisticas.totalPedidos}"/></strong></div>
            <div class="kpi"><span>Revisados</span>
                <strong><c:out value="${empty estadisticas ? '—' : estadisticas.totalRevisados}"/></strong></div>
            <div class="kpi"><span>Pendientes</span>
                <strong><c:out value="${empty estadisticas ? '—' : estadisticas.totalPendientes}"/></strong></div>
            <div class="kpi"><span>% revisado</span>
                <strong>
                    <c:choose>
                        <c:when test="${empty estadisticas}">—</c:when>
                        <c:otherwise>
                            <fmt:formatNumber value="${estadisticas.porcentajeRevisado}"
                                              minFractionDigits="2" maxFractionDigits="2"/> %
                        </c:otherwise>
                    </c:choose>
                </strong></div>
        </div>
    </section>

    <c:if test="${pageContext.request.isUserInRole('OPERADOR')}">
        <section class="tarjeta">
            <h2>Revisar por ID</h2>
            <p class="subtitulo">Útil para validar IDs inválidos o inexistentes, o cuando el servicio de reportes no responde.</p>
            <form method="post" id="form-revision-manual" class="form-grid form-grid-revision"
                  action="${supervisionUrl}/0/revisar" data-base="${supervisionUrl}">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <label>ID del pedido
                    <input name="pedidoId" id="pedidoIdManual" required placeholder="Ej. 15">
                </label>
                <label>Observación
                    <input name="observacion" maxlength="500" placeholder="Motivo de la revisión"
                           value="${fn:escapeXml(observacionIngresada)}">
                </label>
                <button type="submit">Revisar</button>
            </form>
        </section>
    </c:if>

    <section class="tarjeta">
        <div class="titulo-tabla titulo-tabla-acciones">
            <div>
                <h2>Pedidos y estado de revisión</h2>
                <p>Fuente: GET /api/v1/pedidos/revisiones (pedidos-reportes-service).</p>
            </div>
            <form method="get" action="${supervisionUrl}" class="filtro-estado">
                <label>Estado
                    <select name="estado" onchange="this.form.submit()">
                        <option value="TODOS" ${estado == 'TODOS' ? 'selected' : ''}>Todos</option>
                        <option value="PENDIENTE" ${estado == 'PENDIENTE' ? 'selected' : ''}>Pendientes</option>
                        <option value="REVISADO" ${estado == 'REVISADO' ? 'selected' : ''}>Revisados</option>
                    </select>
                </label>
                <noscript><button type="submit">Filtrar</button></noscript>
            </form>
        </div>

        <div class="tabla-scroll">
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Cliente</th>
                    <th>Producto</th>
                    <th>Total</th>
                    <th>Estado</th>
                    <th>Observación / fecha</th>
                    <th>Acción</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="pedido" items="${pedidos}">
                    <tr class="${pedidoRevisadoId == pedido.id ? 'fila-destacada' : ''}">
                        <td><c:out value="${pedido.id}"/></td>
                        <td><c:out value="${pedido.cliente}"/></td>
                        <td><c:out value="${pedido.productoNombre}"/></td>
                        <td>S/ <fmt:formatNumber value="${pedido.total}" minFractionDigits="2" maxFractionDigits="2"/></td>
                        <td>
                            <span class="estado ${pedido.revisado ? 'estado-revisado' : 'estado-pendiente'}">
                                <c:out value="${pedido.estadoRevision}"/>
                            </span>
                        </td>
                        <td>
                            <c:if test="${pedido.revisado}">
                                <c:out value="${pedido.observacion}"/><br>
                                <small><c:out value="${pedido.fechaRevision}"/></small>
                            </c:if>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${pedido.revisado}"><small>Sin acciones</small></c:when>
                                <c:when test="${pageContext.request.isUserInRole('OPERADOR')}">
                                    <form method="post" action="${supervisionUrl}/${pedido.id}/revisar" class="form-revisar">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <input name="observacion" maxlength="500" required placeholder="Observación">
                                        <button type="submit">Revisar</button>
                                    </form>
                                </c:when>
                                <c:otherwise><small>Solo OPERADOR</small></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty pedidos}">
                    <tr><td colspan="7" class="sin-datos">No hay pedidos para mostrar.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>
</div>
<script>
    // Construye la ruta POST /supervision/pedidos/{id}/revisar a partir del ID ingresado.
    (function () {
        var form = document.getElementById('form-revision-manual');
        if (!form) { return; }
        form.addEventListener('submit', function () {
            var id = document.getElementById('pedidoIdManual').value.trim();
            form.action = form.getAttribute('data-base') + '/' + encodeURIComponent(id || 'x') + '/revisar';
        });
    })();
</script>
</body>
</html>
