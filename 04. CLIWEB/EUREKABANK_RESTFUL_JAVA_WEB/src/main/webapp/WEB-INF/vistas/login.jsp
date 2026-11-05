<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ingreso de empleados | EurekaBank</title>
    <link rel="icon" href="<c:url value='/img/marca.svg'/>" type="image/svg+xml">
    <link rel="stylesheet" href="<c:url value='/css/app.css'/>">
    <script src="<c:url value='/js/app.js'/>" defer></script>
</head>
<body>
<main class="acceso">
    <div class="acceso-imagen">
        <img src="<c:url value='/img/Monster.png'/>" alt="Ilustración de Sulley y Boo">
    </div>
    <div class="acceso-panel">
        <div class="tarjeta acceso-tarjeta modulo">
            <div class="acceso-marca">
                <img src="<c:url value='/img/marca.svg'/>" alt="" width="36" height="36">
                <span>EurekaBank</span>
            </div>
            <h1>Ingreso de empleados</h1>
            <form method="post" action="<c:url value='/login'/>">
                <input type="hidden" name="_csrf" value="<c:out value='${csrf}'/>">
                <div>
                    <label for="usuario">Usuario</label>
                    <input id="usuario" name="usuario" type="text" maxlength="64" autocomplete="username" autocapitalize="none" spellcheck="false" autofocus value="<c:out value='${mensaje.usuario}'/>"<c:if test="${mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
                </div>
                <div>
                    <label for="clave">Clave</label>
                    <input id="clave" name="clave" type="password" maxlength="128" autocomplete="current-password">
                </div>
                <button class="boton" type="submit">Ingresar</button>
                <p id="estado" class="estado" role="status" aria-live="polite">
                    <c:if test="${not empty mensaje}">
                        <c:choose>
                            <c:when test="${mensaje.tipo == 'exito'}">
                                <span class="estado-exito"><c:out value="${mensaje.texto}"/></span>
                            </c:when>
                            <c:otherwise>
                                <span class="estado-error"><c:if test="${not empty mensaje.codigo}"><span class="alerta-codigo"><c:out value="${mensaje.codigo}"/></span></c:if><c:out value="${mensaje.texto}"/></span>
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                </p>
            </form>
        </div>
    </div>
</main>
</body>
</html>
