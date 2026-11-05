package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.servlet.CostosServlet;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Test;

/** Módulo costos: el flujo común está en {@link ConsultaServletBase}. */
class CostosServletTest extends ConsultaServletBase {

    private final CostosServlet servlet = new CostosServlet();

    @Override
    HttpServlet servlet() {
        return servlet;
    }

    @Override
    String ruta() {
        return "/costos";
    }

    @Override
    String parametro() {
        return "cuenta";
    }

    @Override
    String modulo() {
        return "costos";
    }

    @Override
    String vista() {
        return "/WEB-INF/vistas/costos.jsp";
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
        return "consultarCostosMantenimiento";
    }
}
