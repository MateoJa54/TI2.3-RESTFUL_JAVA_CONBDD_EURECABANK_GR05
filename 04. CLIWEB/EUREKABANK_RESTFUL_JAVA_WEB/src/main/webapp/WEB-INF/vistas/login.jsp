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
        <section class="tarjeta acceso-tarjeta" aria-labelledby="titulo-acceso">
            <div class="acceso-marca">
                <img src="<c:url value='/img/marca.svg'/>" alt="" width="36" height="36">
                <span>EurekaBank</span>
            </div>
            <div class="encabezado-modulo">
                <h1 id="titulo-acceso">Ingreso de empleados</h1>
                <p>Ingrese con su usuario y su clave de empleado.</p>
            </div>
            <form class="formulario-acceso" method="post" action="<c:url value='/login'/>">
                <input type="hidden" name="_csrf" value="<c:out value='${csrf}'/>">
                <div class="campo">
                    <label for="usuario">Usuario</label>
                    <input id="usuario" name="usuario" type="text" maxlength="64" autocomplete="username" autocapitalize="none" spellcheck="false" autofocus value="<c:out value='${mensaje.usuario}'/>"<c:if test="${mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
                </div>
                <div class="campo">
                    <label for="clave">Clave</label>
                    <div class="campo-clave">
                        <input id="clave" name="clave" type="password" maxlength="128" autocomplete="current-password"<c:if test="${mensaje.tipo == 'error'}"> aria-invalid="true" aria-describedby="estado"</c:if>>
                        <button type="button" class="boton-clave" id="mostrar-clave" aria-controls="clave" aria-pressed="false" hidden>Mostrar</button>
                    </div>
                </div>
                <button class="boton boton-ancho" type="submit">
                    <span class="texto-normal">Ingresar</span>
                    <span class="texto-procesando">Procesando</span>
                </button>
            </form>
            <jsp:include page="/WEB-INF/vistas/_mensaje.jsp"/>
        </section>
    </div>
</main>
</body>
</html>
