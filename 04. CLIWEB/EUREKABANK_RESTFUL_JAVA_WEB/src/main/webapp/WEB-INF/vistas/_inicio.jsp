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
<a class="saltar" href="#contenido">Ir al contenido</a>
<div class="app">
<aside class="lateral">
    <a class="marca" href="<c:url value='/cuentas'/>">
        <img src="<c:url value='/img/marca.svg'/>" alt="" width="32" height="32">
        <span>EurekaBank</span>
    </a>
    <nav class="navegacion" aria-label="Módulos">
        <a href="<c:url value='/cuentas'/>"<c:if test="${modulo == 'cuentas'}"> aria-current="page"</c:if>>
            <svg class="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 6h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z"/><path d="M16 13h2"/></svg>
            <span>Cuentas</span>
        </a>
        <a href="<c:url value='/movimientos'/>"<c:if test="${modulo == 'movimientos'}"> aria-current="page"</c:if>>
            <svg class="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M8 4v16"/><path d="M4 8l4-4 4 4"/><path d="M16 20V4"/><path d="M12 16l4 4 4-4"/></svg>
            <span>Movimientos</span>
        </a>
        <a href="<c:url value='/operacion'/>"<c:if test="${modulo == 'operacion'}"> aria-current="page"</c:if>>
            <svg class="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 8h15"/><path d="M15 4l4 4-4 4"/><path d="M20 16H5"/><path d="M9 12l-4 4 4 4"/></svg>
            <span>Operaciones</span>
        </a>
        <a href="<c:url value='/costos'/>"<c:if test="${modulo == 'costos'}"> aria-current="page"</c:if>>
            <svg class="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M5 3h14v18H5z"/><path d="M8 7h8"/><path d="M8 11h2M11 11h2M14 11h2"/><path d="M8 15h2M11 15h2M14 15h2"/></svg>
            <span>Costos</span>
        </a>
    </nav>
    <div class="lateral-pie">
        <div class="empleado">
            <span class="avatar" aria-hidden="true"><c:out value="${iniciales}"/></span>
            <span class="empleado-datos">
                <span class="empleado-nombre"><c:out value="${usuario.nombreEmpleado}"/></span>
                <span class="empleado-rol"><c:out value="${usuario.rol}"/></span>
            </span>
        </div>
        <form method="post" action="<c:url value='/logout'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${csrf}'/>">
            <button class="boton boton-secundario boton-cerrar" type="submit">
                <svg class="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M14 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4"/><path d="M10 8l-4 4 4 4"/><path d="M6 12h10"/></svg>
                Cerrar sesión
            </button>
        </form>
    </div>
    <form class="lateral-movil" method="post" action="<c:url value='/logout'/>">
        <input type="hidden" name="_csrf" value="<c:out value='${csrf}'/>">
        <button class="boton-salir" type="submit">
            <svg class="icono" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M14 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4"/><path d="M10 8l-4 4 4 4"/><path d="M6 12h10"/></svg>
            <span>Salir</span>
        </button>
    </form>
</aside>
<div class="principal">
<main class="contenido" id="contenido">
