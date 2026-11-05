<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/vistas/_inicio.jsp"><jsp:param name="titulo" value="Operaciones"/></jsp:include>
<div class="modulo">
    <div class="encabezado-modulo">
        <h1>Operaciones</h1>
        <p>Registre un depósito, un retiro o una transferencia.</p>
    </div>
    <form class="tarjeta formulario" method="post" action="<c:url value='/operacion'/>">
        <input type="hidden" name="_csrf" value="<c:out value='${csrf}'/>">
        <input type="hidden" name="envio" value="<c:out value='${envio}'/>">
        <div>
            <label for="modo">Operación</label>
            <select id="modo" name="modo">
                <option value="deposito"<c:if test="${empty mensaje.modo or mensaje.modo == 'deposito'}"> selected</c:if>>Depósito</option>
                <option value="retiro"<c:if test="${mensaje.modo == 'retiro'}"> selected</c:if>>Retiro</option>
                <option value="transferencia"<c:if test="${mensaje.modo == 'transferencia'}"> selected</c:if>>Transferencia</option>
            </select>
        </div>
        <div>
            <label for="cuenta">Cuenta</label>
            <input id="cuenta" name="cuenta" type="text" maxlength="64" autocomplete="off" value="<c:out value='${mensaje.cuenta}'/>"<c:if test="${mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
            <p class="ayuda">En una transferencia es la cuenta de origen.</p>
        </div>
        <div>
            <label for="destino">Cuenta de destino</label>
            <input id="destino" name="destino" type="text" maxlength="64" autocomplete="off" value="<c:out value='${mensaje.destino}'/>">
            <p class="ayuda">Solo para transferencias.</p>
        </div>
        <div>
            <label for="monto">Monto</label>
            <input id="monto" name="monto" type="text" inputmode="decimal" maxlength="40" autocomplete="off" value="<c:out value='${mensaje.monto}'/>">
            <p class="ayuda">Con punto decimal, por ejemplo 150.00.</p>
        </div>
        <div class="formulario-acciones">
            <button class="boton" type="submit">Ejecutar operación</button>
        </div>
    </form>
    <section class="resultado" aria-live="polite" aria-label="Resultado">
        <div class="esqueleto" aria-hidden="true"><span></span><span></span><span></span><span></span></div>
        <div class="resultado-contenido">
            <jsp:include page="/WEB-INF/vistas/_mensaje.jsp"/>
            <c:if test="${empty mensaje}">
                <div class="tarjeta vacio">
                    <h2>Sin operaciones recientes</h2>
                    <p>El resultado de la operación aparece aquí. Revise la cuenta y el monto antes de ejecutarla.</p>
                    <a href="<c:url value='/movimientos'/>">Ver movimientos de una cuenta</a>
                </div>
            </c:if>
        </div>
    </section>
</div>
<jsp:include page="/WEB-INF/vistas/_fin.jsp"/>
