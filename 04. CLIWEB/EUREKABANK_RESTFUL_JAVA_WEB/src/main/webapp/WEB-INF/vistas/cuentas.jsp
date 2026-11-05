<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/vistas/_inicio.jsp"><jsp:param name="titulo" value="Cuentas"/></jsp:include>
<div class="modulo">
    <div class="encabezado-modulo">
        <h1>Cuentas</h1>
        <p>Busque un cliente por su cédula para ver sus cuentas y saldos.</p>
    </div>
    <form class="tarjeta formulario-busqueda" method="get" action="<c:url value='/cuentas'/>">
        <div class="campo">
            <label for="cedula">Cédula del cliente</label>
            <input id="cedula" name="cedula" type="text" maxlength="64" autocomplete="off" value="<c:out value='${consulta}'/>"<c:if test="${not empty mensaje and mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
        </div>
        <button class="boton" type="submit">Buscar</button>
    </form>
    <section class="resultado" aria-live="polite" aria-label="Resultado">
        <div class="tarjeta esqueleto" aria-hidden="true"><span></span><span></span><span></span></div>
        <div class="resultado-contenido">
            <jsp:include page="/WEB-INF/vistas/_mensaje.jsp"/>
            <c:choose>
                <c:when test="${not empty resultado}">
                    <c:choose>
                        <c:when test="${empty resultado.cuentas}">
                            <div class="tarjeta vacio">
                                <svg class="icono vacio-icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 6h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z"/><path d="M16 13h2"/></svg>
                                <h2>Este cliente no tiene cuentas</h2>
                                <p>El cliente existe, pero no tiene cuentas registradas.</p>
                                <a class="boton-texto" href="<c:url value='/cuentas'/>">Hacer otra búsqueda</a>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <dl class="cifras" aria-label="Resumen de cuentas">
                                <div class="tarjeta cifra">
                                    <dt>Cuentas</dt>
                                    <dd><c:out value="${resumen.cantidad}"/></dd>
                                </div>
                                <div class="tarjeta cifra">
                                    <dt>Saldo total por moneda</dt>
                                    <dd>
                                        <c:forEach var="saldo" items="${resumen.saldos}">
                                            <span class="saldo-linea"><fmt:formatNumber value="${saldo.value}" pattern="#,##0.00"/> <small><c:out value="${saldo.key}"/></small></span>
                                        </c:forEach>
                                    </dd>
                                </div>
                            </dl>
                            <p class="encabezado-lista">Cliente <strong><c:out value="${resultado.nombreCliente}"/></strong>, cédula <c:out value="${resultado.cedula}"/></p>
                            <ul class="rejilla-cuentas">
                                <c:forEach var="cuenta" items="${resultado.cuentas}">
                                    <li class="tarjeta tarjeta-cuenta">
                                        <div class="cuenta-cabecera">
                                            <span class="cuenta-codigo"><c:out value="${cuenta.codigo}"/></span>
                                            <c:choose>
                                                <c:when test="${cuenta.estado == 'ACTIVO'}"><span class="chip chip-exito"><c:out value="${cuenta.estado}"/></span></c:when>
                                                <c:otherwise><span class="chip chip-error"><c:out value="${cuenta.estado}"/></span></c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="cuenta-meta">
                                            <span class="chip chip-neutro"><c:out value="${cuenta.descripcionMoneda}"/></span>
                                            <span class="cuenta-cliente">Sucursal <c:out value="${cuenta.codigoSucursal}"/></span>
                                        </div>
                                        <p class="cuenta-saldo"><fmt:formatNumber value="${cuenta.saldo}" pattern="#,##0.00"/></p>
                                        <p class="cuenta-operaciones"><c:choose><c:when test="${cuenta.operacionesRealizadas == 1}">1 operación</c:when><c:otherwise><c:out value="${cuenta.operacionesRealizadas}"/> operaciones</c:otherwise></c:choose></p>
                                        <nav class="cuenta-acciones" aria-label="Acciones de la cuenta">
                                            <a class="boton boton-secundario" data-carga href="<c:url value='/movimientos'><c:param name='cuenta' value='${cuenta.codigo}'/></c:url>">Ver movimientos</a>
                                            <a class="boton boton-secundario" data-carga href="<c:url value='/costos'><c:param name='cuenta' value='${cuenta.codigo}'/></c:url>">Ver costos</a>
                                            <a class="boton boton-secundario" data-carga href="<c:url value='/operacion'><c:param name='cuenta' value='${cuenta.codigo}'/></c:url>">Operar</a>
                                        </nav>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:when test="${empty mensaje}">
                    <div class="tarjeta vacio">
                        <svg class="icono vacio-icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 6h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z"/><path d="M16 13h2"/></svg>
                        <h2>Busque un cliente</h2>
                        <p>Escriba la cédula del cliente para ver sus cuentas y saldos.</p>
                        <a class="boton-texto" href="<c:url value='/cuentas'/>?cedula=06914897">Usar la cédula de ejemplo 06914897</a>
                    </div>
                </c:when>
            </c:choose>
        </div>
    </section>
</div>
<jsp:include page="/WEB-INF/vistas/_fin.jsp"/>
