<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/vistas/_inicio.jsp"><jsp:param name="titulo" value="Cuentas"/></jsp:include>
<div class="modulo">
    <div class="encabezado-modulo">
        <h1>Cuentas</h1>
        <p>Consulte las cuentas de un cliente por su cédula.</p>
    </div>
    <form class="tarjeta formulario" method="get" action="<c:url value='/cuentas'/>">
        <div>
            <label for="cedula">Cédula del cliente</label>
            <input id="cedula" name="cedula" type="text" maxlength="64" autocomplete="off" value="<c:out value='${consulta}'/>"<c:if test="${not empty mensaje and mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
        </div>
        <div class="formulario-acciones">
            <button class="boton" type="submit">Buscar</button>
        </div>
    </form>
    <section class="resultado" aria-live="polite" aria-label="Resultado">
        <div class="esqueleto" aria-hidden="true"><span></span><span></span><span></span><span></span></div>
        <div class="resultado-contenido">
            <jsp:include page="/WEB-INF/vistas/_mensaje.jsp"/>
            <c:choose>
                <c:when test="${not empty resultado}">
                    <c:choose>
                        <c:when test="${empty resultado.cuentas}">
                            <div class="tarjeta vacio">
                                <h2>Este cliente no tiene cuentas</h2>
                                <p>El cliente existe, pero no tiene cuentas registradas.</p>
                                <a href="<c:url value='/cuentas'/>">Hacer otra búsqueda</a>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="tarjeta">
                                <p class="resumen">Cliente <strong><c:out value="${resultado.nombreCliente}"/></strong>, cédula <c:out value="${resultado.cedula}"/></p>
                                <div class="tabla-contenedor">
                                    <table>
                                        <thead>
                                            <tr>
                                                <th scope="col">Cuenta</th>
                                                <th scope="col">Moneda</th>
                                                <th scope="col">Estado</th>
                                                <th scope="col">Sucursal</th>
                                                <th scope="col">Apertura</th>
                                                <th scope="col" class="num">Operaciones</th>
                                                <th scope="col" class="num">Saldo</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="cuenta" items="${resultado.cuentas}">
                                                <tr>
                                                    <td><c:out value="${cuenta.codigo}"/></td>
                                                    <td><c:out value="${cuenta.descripcionMoneda}"/></td>
                                                    <td><c:out value="${cuenta.estado}"/></td>
                                                    <td><c:out value="${cuenta.codigoSucursal}"/></td>
                                                    <td><c:out value="${cuenta.fechaCreacion}"/></td>
                                                    <td class="num"><c:out value="${cuenta.operacionesRealizadas}"/></td>
                                                    <td class="num"><fmt:formatNumber value="${cuenta.saldo}" pattern="#,##0.00"/></td>
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
                        <h2>Busque un cliente</h2>
                        <p>Escriba la cédula del cliente para ver sus cuentas y saldos.</p>
                        <a href="#cedula">Ir al campo de cédula</a>
                    </div>
                </c:when>
            </c:choose>
        </div>
    </section>
</div>
<jsp:include page="/WEB-INF/vistas/_fin.jsp"/>
