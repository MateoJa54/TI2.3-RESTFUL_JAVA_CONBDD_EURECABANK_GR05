<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:if test="${not empty mensaje}">
    <c:choose>
        <c:when test="${mensaje.tipo == 'exito'}">
            <div id="estado" class="alerta alerta-exito" role="status">
                <c:if test="${not empty mensaje.codigo}"><span class="alerta-codigo"><c:out value="${mensaje.codigo}"/></span></c:if>
                <span><c:out value="${mensaje.texto}"/></span>
                <c:if test="${not empty mensaje.saldoNuevo}">
                    <dl class="alerta-detalle">
                        <div><dt>Saldo nuevo</dt><dd><fmt:formatNumber value="${mensaje.saldoNuevo}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Costo aplicado</dt><dd><fmt:formatNumber value="${mensaje.costoAplicado}" pattern="#,##0.00"/></dd></div>
                        <div><dt>ITF aplicado</dt><dd><fmt:formatNumber value="${mensaje.itfAplicado}" pattern="#,##0.00"/></dd></div>
                    </dl>
                </c:if>
                <c:if test="${not empty mensaje.saldoOrigen}">
                    <dl class="alerta-detalle">
                        <div><dt>Saldo de origen</dt><dd><fmt:formatNumber value="${mensaje.saldoOrigen}" pattern="#,##0.00"/></dd></div>
                        <div><dt>Saldo de destino</dt><dd><fmt:formatNumber value="${mensaje.saldoDestino}" pattern="#,##0.00"/></dd></div>
                        <div><dt>ITF aplicado</dt><dd><fmt:formatNumber value="${mensaje.itfAplicado}" pattern="#,##0.00"/></dd></div>
                    </dl>
                </c:if>
            </div>
        </c:when>
        <c:otherwise>
            <div id="estado" class="alerta alerta-error" role="alert">
                <c:if test="${not empty mensaje.codigo}"><span class="alerta-codigo"><c:out value="${mensaje.codigo}"/></span></c:if>
                <span><c:out value="${mensaje.texto}"/></span>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>
