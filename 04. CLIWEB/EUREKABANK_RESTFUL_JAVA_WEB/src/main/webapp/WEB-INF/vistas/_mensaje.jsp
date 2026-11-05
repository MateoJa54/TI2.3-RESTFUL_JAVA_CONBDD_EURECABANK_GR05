<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Aviso de una sola vez. El texto y el código son los del servidor: se escapan, nunca se reescriben. --%>
<c:if test="${not empty mensaje}">
    <c:choose>
        <c:when test="${mensaje.tipo == 'exito'}">
            <div id="estado" class="aviso aviso-exito" role="status">
                <svg class="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="9"/><path d="M8 12.5l2.5 2.5L16 9.5"/></svg>
                <div class="aviso-texto">
                    <p><c:if test="${not empty mensaje.codigo}"><span class="aviso-codigo"><c:out value="${mensaje.codigo}"/></span></c:if><c:out value="${mensaje.texto}"/></p>
                </div>
                <button type="button" class="aviso-cerrar" data-descartar hidden>Cerrar aviso</button>
            </div>
        </c:when>
        <c:otherwise>
            <div id="estado" class="aviso aviso-error" role="alert">
                <svg class="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="9"/><path d="M12 7.5v5.5"/><path d="M12 16.5v.01"/></svg>
                <div class="aviso-texto">
                    <p><c:if test="${not empty mensaje.codigo}"><span class="aviso-codigo"><c:out value="${mensaje.codigo}"/></span></c:if><c:out value="${mensaje.texto}"/></p>
                </div>
                <button type="button" class="aviso-cerrar" data-descartar hidden>Cerrar aviso</button>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>
