<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="codigo" value="${not empty requestScope['jakarta.servlet.error.status_code'] ? requestScope['jakarta.servlet.error.status_code'] : estado}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>No se pudo completar la solicitud | EurekaBank</title>
    <link rel="icon" href="<c:url value='/img/marca.svg'/>" type="image/svg+xml">
    <link rel="stylesheet" href="<c:url value='/css/app.css'/>">
</head>
<body>
<main class="pagina-error">
    <div class="tarjeta">
        <c:choose>
            <c:when test="${codigo == 403}"><h1>Solicitud no verificada</h1></c:when>
            <c:when test="${codigo == 404}"><h1>Página no encontrada</h1></c:when>
            <c:when test="${codigo == 405}"><h1>Operación no permitida</h1></c:when>
            <c:otherwise><h1>No se pudo completar la solicitud</h1></c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${not empty detalle}"><p><c:out value="${detalle}"/></p></c:when>
            <c:when test="${codigo == 404}"><p>La página que busca no existe.</p></c:when>
            <c:when test="${codigo == 405}"><p>Esta dirección no admite ese tipo de petición.</p></c:when>
            <c:otherwise><p>Ocurrió un problema al atender la solicitud. Intente de nuevo en unos minutos.</p></c:otherwise>
        </c:choose>
        <a class="boton" href="<c:url value='/'/>">Volver al inicio</a>
    </div>
</main>
</body>
</html>
