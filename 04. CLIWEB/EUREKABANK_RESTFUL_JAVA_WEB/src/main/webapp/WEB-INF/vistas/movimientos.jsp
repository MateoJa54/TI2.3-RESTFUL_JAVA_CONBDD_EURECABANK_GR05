<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/vistas/_inicio.jsp"><jsp:param name="titulo" value="Movimientos"/></jsp:include>
<div class="modulo">
    <div class="encabezado-modulo">
        <h1>Movimientos</h1>
        <p>Consulte el libro de movimientos de una cuenta.</p>
    </div>
    <form class="tarjeta formulario" method="get" action="<c:url value='/movimientos'/>">
        <div>
            <label for="cuenta">Código de la cuenta</label>
            <input id="cuenta" name="cuenta" type="text" maxlength="64" autocomplete="off" value="<c:out value='${consulta}'/>"<c:if test="${not empty mensaje and mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
        </div>
        <div class="formulario-acciones">
            <button class="boton" type="submit">Consultar</button>
        </div>
    </form>
    <section class="resultado" aria-live="polite" aria-label="Resultado">
        <div class="esqueleto" aria-hidden="true"><span></span><span></span><span></span><span></span></div>
        <div class="resultado-contenido">
            <jsp:include page="/WEB-INF/vistas/_mensaje.jsp"/>
            <c:choose>
                <c:when test="${not empty resultado}">
                    <c:choose>
                        <c:when test="${empty resultado.movimientos}">
                            <div class="tarjeta vacio">
                                <h2>La cuenta no tiene movimientos</h2>
                                <p>Todavía no se registra ninguna operación en esta cuenta.</p>
                                <a href="<c:url value='/operacion'/>">Ir a Operaciones</a>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="tarjeta">
                                <p class="resumen">Cuenta <strong><c:out value="${resultado.cuenta}"/></strong></p>
                                <div class="tabla-contenedor">
                                    <table>
                                        <thead>
                                            <tr>
                                                <th scope="col" class="num">N.°</th>
                                                <th scope="col">Fecha</th>
                                                <th scope="col">Tipo</th>
                                                <th scope="col">Acción</th>
                                                <th scope="col">Referencia</th>
                                                <th scope="col" class="num">Importe</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="movimiento" items="${resultado.movimientos}">
                                                <tr>
                                                    <td class="num"><c:out value="${movimiento.numero}"/></td>
                                                    <td><c:out value="${movimiento.fecha}"/></td>
                                                    <td><c:out value="${movimiento.tipoDescripcion}"/></td>
                                                    <td><c:out value="${movimiento.tipoAccion}"/></td>
                                                    <td><c:out value="${movimiento.referencia}"/></td>
                                                    <td class="num"><fmt:formatNumber value="${movimiento.importe}" pattern="#,##0.00"/></td>
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
                        <h2>Elija una cuenta</h2>
                        <p>Escriba el código de la cuenta para ver sus movimientos.</p>
                        <a href="#cuenta">Ir al campo de cuenta</a>
                    </div>
                </c:when>
            </c:choose>
        </div>
    </section>
</div>
<jsp:include page="/WEB-INF/vistas/_fin.jsp"/>
