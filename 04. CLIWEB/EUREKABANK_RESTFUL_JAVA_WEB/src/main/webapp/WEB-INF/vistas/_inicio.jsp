<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="en_US" scope="request"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${param.titulo}"/> | EurekaBank</title>
    <link rel="icon" href="<c:url value='/img/marca.svg'/>" type="image/svg+xml">
    <link rel="stylesheet" href="<c:url value='/css/app.css'/>">
    <script src="<c:url value='/js/app.js'/>" defer></script>
</head>
<body>
<header class="barra">
    <a class="marca" href="<c:url value='/cuentas'/>">
        <img src="<c:url value='/img/marca.svg'/>" alt="" width="32" height="32">
        <span>EurekaBank</span>
    </a>
    <nav class="navegacion" aria-label="Módulos">
        <a href="<c:url value='/cuentas'/>"<c:if test="${modulo == 'cuentas'}"> aria-current="page"</c:if>>Cuentas</a>
        <a href="<c:url value='/movimientos'/>"<c:if test="${modulo == 'movimientos'}"> aria-current="page"</c:if>>Movimientos</a>
        <a href="<c:url value='/operacion'/>"<c:if test="${modulo == 'operacion'}"> aria-current="page"</c:if>>Operaciones</a>
        <a href="<c:url value='/costos'/>"<c:if test="${modulo == 'costos'}"> aria-current="page"</c:if>>Costos</a>
    </nav>
    <div class="sesion">
        <div class="sesion-datos">
            <span class="sesion-nombre"><c:out value="${usuario.nombreEmpleado}"/></span>
            <span class="sesion-rol"><c:out value="${usuario.rol}"/></span>
        </div>
        <form method="post" action="<c:url value='/logout'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${csrf}'/>">
            <button class="boton boton-secundario" type="submit">Cerrar sesión</button>
        </form>
    </div>
</header>
<main class="contenido">
