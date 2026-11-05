package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.servlet.CuentasServlet;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Test;

/** Módulo cuentas: el flujo común está en {@link ConsultaServletBase}. */
class CuentasServletTest extends ConsultaServletBase {

    private final CuentasServlet servlet = new CuentasServlet();

    @Override
    HttpServlet servlet() {
        return servlet;
    }

    @Override
    String ruta() {
        return "/cuentas";
    }

    @Override
    String parametro() {
        return "cedula";
    }

    @Override
    String modulo() {
        return "cuentas";
    }

    @Override
    String vista() {
        return "/WEB-INF/vistas/cuentas.jsp";
    }

    @Override
    String valorExistente() {
        return ClienteFalso.CEDULA;
    }

    @Override
    String codigoInexistente() {
        return "CLI-001";
    }

    @Override
    String mensajeInexistente() {
        return "El cliente solicitado no existe";
    }

    @Override
    String metodoDelServicio() {
        return "listarCuentasPorCliente";
    }
}
