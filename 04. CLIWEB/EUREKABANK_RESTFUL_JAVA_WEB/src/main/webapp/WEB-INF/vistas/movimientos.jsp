<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/vistas/_inicio.jsp"><jsp:param name="titulo" value="Movimientos"/></jsp:include>
<div class="modulo">
    <div class="encabezado-modulo">
        <h1>Movimientos</h1>
        <p>Consulte el libro de movimientos de una cuenta.</p>
    </div>
    <form class="tarjeta formulario-busqueda" method="get" action="<c:url value='/movimientos'/>">
        <div class="campo">
            <label for="cuenta">Código de la cuenta</label>
            <input id="cuenta" name="cuenta" type="text" maxlength="64" autocomplete="off" value="<c:out value='${consulta}'/>"<c:if test="${not empty mensaje and mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
        </div>
        <button class="boton" type="submit">Consultar</button>
    </form>
    <section class="resultado" aria-live="polite" aria-label="Resultado">
        <div class="tarjeta esqueleto" aria-hidden="true"><span></span><span></span><span></span></div>
        <div class="resultado-contenido">
            <jsp:include page="/WEB-INF/vistas/_mensaje.jsp"/>
            <c:choose>
                <c:when test="${not empty resultado}">
                    <c:choose>
                        <c:when test="${empty resultado.movimientos}">
                            <div class="tarjeta vacio">
                                <svg class="icono vacio-icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M8 4v16"/><path d="M4 8l4-4 4 4"/><path d="M16 20V4"/><path d="M12 16l4 4 4-4"/></svg>
                                <h2>Esta cuenta aún no tiene movimientos</h2>
                                <p>Cuando se registre un depósito, un retiro o una transferencia, aparecerá aquí.</p>
                                <a class="boton-texto" href="<c:url value='/operacion'/>">Ir a Operaciones</a>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <dl class="cifras" aria-label="Resumen de movimientos">
                                <div class="tarjeta cifra">
                                    <dt>Ingresos</dt>
                                    <dd class="importe-ingreso">+<fmt:formatNumber value="${resumen.ingresos}" pattern="#,##0.00"/></dd>
                                </div>
                                <div class="tarjeta cifra">
                                    <dt>Salidas</dt>
                                    <dd class="importe-salida">-<fmt:formatNumber value="${resumen.salidas}" pattern="#,##0.00"/></dd>
                                </div>
                                <div class="tarjeta cifra">
                                    <dt>Movimientos</dt>
                                    <dd><c:out value="${resumen.cantidad}"/></dd>
                                </div>
                            </dl>
                            <div class="tarjeta tabla-tarjeta">
                                <p class="tabla-cabecera">Cuenta <strong><c:out value="${resultado.cuenta}"/></strong></p>
                                <div class="tabla-contenedor">
                                    <table>
                                        <caption class="solo-lector">Movimientos de la cuenta <c:out value="${resultado.cuenta}"/></caption>
                                        <thead>
                                            <tr>
                                                <th scope="col" class="num">Nº</th>
                                                <th scope="col">Fecha</th>
                                                <th scope="col">Tipo</th>
                                                <th scope="col">Descripción</th>
                                                <th scope="col">Referencia</th>
                                                <th scope="col">Empleado</th>
                                                <th scope="col" class="num">Importe</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="movimiento" items="${resultado.movimientos}">
                                                <tr>
                                                    <td class="num"><c:out value="${movimiento.numero}"/></td>
                                                    <td><c:out value="${movimiento.fecha}"/></td>
                                                    <td>
                                                        <c:choose>
                                                            <c:when test="${movimiento.tipoAccion == 'INGRESO'}"><span class="chip chip-exito">Ingreso</span></c:when>
                                                            <c:when test="${movimiento.tipoAccion == 'SALIDA'}"><span class="chip chip-error">Salida</span></c:when>
                                                            <c:otherwise><span class="chip chip-neutro"><c:out value="${movimiento.tipoAccion}"/></span></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <td><c:out value="${movimiento.tipoDescripcion}"/></td>
                                                    <td><c:out value="${movimiento.referencia}"/></td>
                                                    <td><c:out value="${movimiento.empleado}"/></td>
                                                    <td class="num">
                                                        <c:choose>
                                                            <c:when test="${movimiento.tipoAccion == 'INGRESO'}"><span class="importe-ingreso">+<fmt:formatNumber value="${movimiento.importe}" pattern="#,##0.00"/></span></c:when>
                                                            <c:when test="${movimiento.tipoAccion == 'SALIDA'}"><span class="importe-salida">-<fmt:formatNumber value="${movimiento.importe}" pattern="#,##0.00"/></span></c:when>
                                                            <c:otherwise><fmt:formatNumber value="${movimiento.importe}" pattern="#,##0.00"/></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:when test="${empty mensaje}">
                    <div class="tarjeta vacio">
                        <svg class="icono vacio-icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M8 4v16"/><path d="M4 8l4-4 4 4"/><path d="M16 20V4"/><path d="M12 16l4 4 4-4"/></svg>
                        <h2>Elija una cuenta</h2>
                        <p>Escriba el código de la cuenta para ver sus movimientos.</p>
                    </div>
                </c:when>
            </c:choose>
        </div>
    </section>
</div>
<jsp:include page="/WEB-INF/vistas/_fin.jsp"/>
