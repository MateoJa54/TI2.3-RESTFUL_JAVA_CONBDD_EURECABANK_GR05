/* Mejora progresiva: la aplicación funciona sin este archivo.
   Al enviar un formulario deshabilita el botón (evita el doble envío) y muestra el esqueleto. */
(function () {
    'use strict';

    document.addEventListener('submit', function (evento) {
        var formulario = evento.target;
        if (!(formulario instanceof HTMLFormElement)) {
            return;
        }
        var modulo = formulario.closest('.modulo');
        if (modulo) {
            modulo.classList.add('cargando');
        }
        var boton = formulario.querySelector('button[type="submit"]');
        if (boton) {
            window.setTimeout(function () {
                boton.disabled = true;
            }, 0);
        }
    });

    window.addEventListener('pageshow', function (evento) {
        if (!evento.persisted) {
            return;
        }
        var cargando = document.querySelectorAll('.cargando');
        for (var i = 0; i < cargando.length; i++) {
            cargando[i].classList.remove('cargando');
        }
        var botones = document.querySelectorAll('button[type="submit"]:disabled');
        for (var j = 0; j < botones.length; j++) {
            botones[j].disabled = false;
        }
    });
}());
