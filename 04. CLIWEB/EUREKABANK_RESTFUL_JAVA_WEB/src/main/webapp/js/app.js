/* Mejora progresiva: la aplicación funciona sin este archivo. Los controles que aquí aparecen
   empiezan ocultos (atributo hidden) y solo se muestran si este script corre.
   - Al enviar un formulario deshabilita el botón (evita el doble envío), cambia el texto a
     "Procesando" y muestra el esqueleto de carga.
   - Mostrar u ocultar la clave, con aria-pressed.
   - Descartar un aviso. */
(function () {
    'use strict';

    function alEnviar(evento) {
        var formulario = evento.target;
        if (!(formulario instanceof HTMLFormElement)) {
            return;
        }
        formulario.classList.add('enviando');
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
    }

    function alPulsarEnlace(evento) {
        if (evento.defaultPrevented || evento.button !== 0 ||
                evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) {
            return;
        }
        var destino = evento.target instanceof Element ? evento.target.closest('a[data-carga]') : null;
        if (!destino) {
            return;
        }
        var modulo = destino.closest('.modulo');
        if (modulo) {
            modulo.classList.add('cargando');
        }
    }

    function iniciarMostrarClave() {
        var boton = document.getElementById('mostrar-clave');
        var clave = document.getElementById('clave');
        if (!boton || !clave) {
            return;
        }
        boton.hidden = false;
        boton.addEventListener('click', function () {
            var mostrando = clave.type === 'text';
            clave.type = mostrando ? 'password' : 'text';
            boton.setAttribute('aria-pressed', mostrando ? 'false' : 'true');
            boton.textContent = mostrando ? 'Mostrar' : 'Ocultar';
        });
    }

    function iniciarDescartarAvisos() {
        var botones = document.querySelectorAll('[data-descartar]');
        for (var i = 0; i < botones.length; i++) {
            botones[i].hidden = false;
            botones[i].addEventListener('click', function (evento) {
                var aviso = evento.currentTarget.closest('.aviso');
                if (aviso) {
                    aviso.hidden = true;
                }
            });
        }
    }

    function restaurarEstado() {
        var marcados = document.querySelectorAll('.cargando, .enviando');
        for (var i = 0; i < marcados.length; i++) {
            marcados[i].classList.remove('cargando');
            marcados[i].classList.remove('enviando');
        }
        var botones = document.querySelectorAll('button[type="submit"]:disabled');
        for (var j = 0; j < botones.length; j++) {
            botones[j].disabled = false;
        }
    }

    document.addEventListener('submit', alEnviar);
    document.addEventListener('click', alPulsarEnlace);

    window.addEventListener('pageshow', function (evento) {
        if (evento.persisted) {
            restaurarEstado();
        }
    });

    iniciarMostrarClave();
    iniciarDescartarAvisos();
}());
