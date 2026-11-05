<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/vistas/_inicio.jsp"><jsp:param name="titulo" value="Costos"/></jsp:include>
<div class="modulo">
    <div class="encabezado-modulo">
        <h1>Costos</h1>
        <p>Consulte la proyección de costos de mantenimiento de una cuenta.</p>
    </div>
    <form class="tarjeta formulario" method="get" action="<c:url value='/costos'/>">
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
                    <dl class="detalle">
                        <div><dt>Saldo actual</dt><dd><fmt:formatNumber value="${resultado.saldoActual}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Costo de mantenimiento</dt><dd><fmt:formatNumber value="${resultado.costoMantenimiento}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Costo por movimiento</dt><dd><fmt:formatNumber value="${resultado.costoMovimiento}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Interés proyectado</dt><dd><fmt:formatNumber value="${resultado.interesProyectado}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Límite de mantenimiento</dt><dd><fmt:formatNumber value="${resultado.limiteMantenimiento}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Operaciones sin costo</dt><dd><c:out value="${resultado.operacionesSinCosto}"/></dd></div>
                    </dl>
                </c:when>
                <c:when test="${empty mensaje}">
                    <div class="tarjeta vacio">
                        <h2>Elija una cuenta</h2>
                        <p>Escriba el código de la cuenta para ver su proyección de costos.</p>
                        <a href="#cuenta">Ir al campo de cuenta</a>
                    </div>
                </c:when>
            </c:choose>
        </div>
    </section>
</div>
<jsp:include page="/WEB-INF/vistas/_fin.jsp"/>
