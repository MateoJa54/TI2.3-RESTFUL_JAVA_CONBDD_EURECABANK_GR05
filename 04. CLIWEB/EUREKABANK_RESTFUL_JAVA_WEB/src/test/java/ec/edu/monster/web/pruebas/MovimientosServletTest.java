package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.servlet.MovimientosServlet;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Test;

/** Módulo movimientos: el flujo común está en {@link ConsultaServletBase}. */
class MovimientosServletTest extends ConsultaServletBase {

    private final MovimientosServlet servlet = new MovimientosServlet();

    @Override
    HttpServlet servlet() {
        return servlet;
    }

    @Override
    String ruta() {
        return "/movimientos";
    }

    @Override
    String parametro() {
        return "cuenta";
    }

    @Override
    String modulo() {
        return "movimientos";
    }

    @Override
    String vista() {
        return "/WEB-INF/vistas/movimientos.jsp";
    }

    @Override
    String valorExistente() {
        return ClienteFalso.CUENTA;
    }

    @Override
    String codigoInexistente() {
        return "CTA-001";
    }

    @Override
    String mensajeInexistente() {
        return "La cuenta solicitada no existe";
    }

    @Override
    String metodoDelServicio() {
        return "consultarMovimientos";
    }
}
