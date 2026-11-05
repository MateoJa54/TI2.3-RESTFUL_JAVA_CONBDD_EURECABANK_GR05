<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/vistas/_inicio.jsp"><jsp:param name="titulo" value="Operaciones"/></jsp:include>
<div class="modulo">
    <div class="encabezado-modulo">
        <h1>Operaciones</h1>
        <p>Registre un depósito, un retiro o una transferencia.</p>
    </div>
    <div class="operacion">
        <form id="operacion" class="tarjeta formulario-operacion" method="post" action="<c:url value='/operacion'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${csrf}'/>">
            <input type="hidden" name="envio" value="<c:out value='${envio}'/>">
            <fieldset>
                <legend>Tipo de operación</legend>
                <div class="segmentado">
                    <input type="radio" id="modo-deposito" name="modo" value="deposito"<c:if test="${empty mensaje.modo or mensaje.modo == 'deposito'}"> checked</c:if>>
                    <label for="modo-deposito">Depósito</label>
                    <input type="radio" id="modo-retiro" name="modo" value="retiro"<c:if test="${mensaje.modo == 'retiro'}"> checked</c:if>>
                    <label for="modo-retiro">Retiro</label>
                    <input type="radio" id="modo-transferencia" name="modo" value="transferencia"<c:if test="${mensaje.modo == 'transferencia'}"> checked</c:if>>
                    <label for="modo-transferencia">Transferencia</label>
                </div>
            </fieldset>
            <div class="campo">
                <label for="cuenta">Cuenta</label>
                <input id="cuenta" name="cuenta" type="text" maxlength="64" autocomplete="off" value="<c:out value='${consulta}'/>"<c:if test="${mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
                <p class="ayuda">En una transferencia es la cuenta de origen.</p>
            </div>
            <div class="campo campo-destino">
                <label for="destino">Cuenta de destino</label>
                <input id="destino" name="destino" type="text" maxlength="64" autocomplete="off" value="<c:out value='${mensaje.destino}'/>">
                <p class="ayuda">Solo se usa en las transferencias.</p>
            </div>
            <div class="campo">
                <label for="monto">Monto</label>
                <div class="prefijo-monto">
                    <span class="prefijo" aria-hidden="true">$</span>
                    <input id="monto" name="monto" type="text" inputmode="decimal" maxlength="40" autocomplete="off" value="<c:out value='${mensaje.monto}'/>" aria-describedby="ayuda-monto">
                </div>
                <p class="ayuda" id="ayuda-monto">Máximo dos decimales, con punto. Por ejemplo 150.00.</p>
            </div>
            <div>
                <button class="boton boton-operacion" type="submit">
                    <span class="texto-generico">Ejecutar operación</span>
                    <span class="texto-accion texto-depositar">Depositar</span>
                    <span class="texto-accion texto-retirar">Retirar</span>
                    <span class="texto-accion texto-transferir">Transferir</span>
                </button>
            </div>
        </form>
        <aside class="tarjeta reglas" aria-labelledby="reglas-titulo">
            <h2 id="reglas-titulo">Antes de confirmar</h2>
            <ul>
                <li>El monto debe ser mayor que cero.</li>
                <li>Use punto decimal, con máximo dos decimales.</li>
                <li>El costo por movimiento y el ITF los calcula el servidor.</li>
                <li>En una transferencia, la cuenta de destino debe ser distinta de la de origen.</li>
            </ul>
        </aside>
    </div>
    <section class="resultado" aria-live="polite" aria-label="Resultado">
        <div class="tarjeta esqueleto" aria-hidden="true"><span></span><span></span><span></span></div>
        <div class="resultado-contenido">
            <jsp:include page="/WEB-INF/vistas/_mensaje.jsp"/>
            <c:if test="${mensaje.tipo == 'exito' and not empty mensaje.saldoNuevo}">
                <section class="tarjeta resultado-operacion" aria-label="Datos de la operación">
                    <dl class="datos">
                        <div><dt>Número de movimiento</dt><dd><c:out value="${mensaje.numero}"/></dd></div>
                        <div><dt>Saldo nuevo</dt><dd><fmt:formatNumber value="${mensaje.saldoNuevo}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Costo aplicado</dt><dd><fmt:formatNumber value="${mensaje.costoAplicado}" pattern="#,##0.00"/></dd></div>
                        <div><dt>ITF aplicado</dt><dd><fmt:formatNumber value="${mensaje.itfAplicado}" pattern="#,##0.00"/></dd></div>
                    </dl>
                </section>
            </c:if>
            <c:if test="${mensaje.tipo == 'exito' and not empty mensaje.saldoOrigen}">
                <section class="tarjeta resultado-operacion" aria-label="Datos de la transferencia">
                    <dl class="datos">
                        <div><dt>Movimiento de origen</dt><dd><c:out value="${mensaje.numeroOrigen}"/></dd></div>
                        <div><dt>Movimiento de destino</dt><dd><c:out value="${mensaje.numeroDestino}"/></dd></div>
                        <div><dt>Saldo de origen</dt><dd><fmt:formatNumber value="${mensaje.saldoOrigen}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Saldo de destino</dt><dd><fmt:formatNumber value="${mensaje.saldoDestino}" pattern="#,##0.00"/></dd></div>
                        <div><dt>ITF aplicado</dt><dd><fmt:formatNumber value="${mensaje.itfAplicado}" pattern="#,##0.00"/></dd></div>
                    </dl>
                </section>
            </c:if>
            <c:if test="${empty mensaje}">
                <div class="tarjeta vacio">
                    <svg class="icono vacio-icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 8h15"/><path d="M15 4l4 4-4 4"/><path d="M20 16H5"/><path d="M9 12l-4 4 4 4"/></svg>
                    <h2>Sin operaciones recientes</h2>
                    <p>El resultado de la operación aparece aquí. Revise la cuenta y el monto antes de ejecutarla.</p>
                    <a class="boton-texto" href="<c:url value='/movimientos'/>">Ver movimientos de una cuenta</a>
                </div>
            </c:if>
        </div>
    </section>
</div>
<jsp:include page="/WEB-INF/vistas/_fin.jsp"/>
