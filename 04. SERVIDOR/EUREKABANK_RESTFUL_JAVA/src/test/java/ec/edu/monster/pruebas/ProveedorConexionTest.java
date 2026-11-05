package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.datos.ProveedorConexion;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la resolucion de la configuracion del servidor.
 */
class ProveedorConexionTest {

    @Test
    void unaPropiedadDelSistemaGanaAlFicheroDePropiedades() {
        String anterior = System.getProperty("eurekabank.db.usuario");
        System.setProperty("eurekabank.db.usuario", "usuario-de-prueba");
        try {
            assertEquals("usuario-de-prueba",
                    ProveedorConexion.leer("VARIABLE_QUE_NO_EXISTE", "eurekabank.db.usuario",
                            "eurekabank.db.usuario", "por-defecto"));
        } finally {
            if (anterior == null) {
                System.clearProperty("eurekabank.db.usuario");
            } else {
                System.setProperty("eurekabank.db.usuario", anterior);
            }
        }
    }

    @Test
    void elFicheroDePropiedadesGanaAlValorPorDefecto() {
        String valor = ProveedorConexion.leer("VARIABLE_QUE_NO_EXISTE",
                "propiedad.que.no.existe", "eurekabank.db.url", "valor-por-defecto");

        assertTrue(valor.startsWith("jdbc:mysql://"), valor);
    }

    @Test
    void sinNingunaFuenteSeUsaElValorPorDefecto() {
        assertEquals("valor-por-defecto", ProveedorConexion.leer("VARIABLE_QUE_NO_EXISTE",
                "propiedad.que.no.existe", "clave.que.no.existe", "valor-por-defecto"));
    }

    @Test
    void unValorEnBlancoSeIgnoraYSeBuscaEnLaFuenteSiguiente() {
        String anterior = System.getProperty("eurekabank.db.clave");
        System.setProperty("eurekabank.db.clave", "   ");
        try {
            assertEquals(ProveedorConexion.CLAVE_POR_DEFECTO,
                    ProveedorConexion.leer("VARIABLE_QUE_NO_EXISTE", "eurekabank.db.clave",
                            "clave.que.no.existe", ProveedorConexion.CLAVE_POR_DEFECTO));
        } finally {
            if (anterior == null) {
                System.clearProperty("eurekabank.db.clave");
            } else {
                System.setProperty("eurekabank.db.clave", anterior);
            }
        }
    }

    @Test
    void elProveedorPorDefectoUsaLaConfiguracionDelEntorno() {
        ProveedorConexion proveedor = new ProveedorConexion();

        assertNotNull(proveedor.getUrl());
        assertNotNull(proveedor.getUsuario());
        assertTrue(proveedor.getUrl().startsWith("jdbc:mysql://"), proveedor.getUrl());
    }

    @Test
    void elProveedorAceptaUnaConfiguracionExplicita() {
        ProveedorConexion proveedor =
                new ProveedorConexion("jdbc:mysql://127.0.0.1:3307/eurekabank", "root", "");

        assertEquals("jdbc:mysql://127.0.0.1:3307/eurekabank", proveedor.getUrl());
        assertEquals("root", proveedor.getUsuario());
    }

    @Test
    void losNombresDeLasVariablesDeEntornoSonLosDelContrato() {
        assertEquals("EUREKABANK_DB_URL", ProveedorConexion.VARIABLE_URL);
        assertEquals("EUREKABANK_DB_USUARIO", ProveedorConexion.VARIABLE_USUARIO);
        assertEquals("EUREKABANK_DB_CLAVE", ProveedorConexion.VARIABLE_CLAVE);
    }

    @Test
    void losValoresPorDefectoSonLosDelContrato() {
        assertEquals("eurekabank_app", ProveedorConexion.USUARIO_POR_DEFECTO);
        assertEquals("EurekaBank2026!", ProveedorConexion.CLAVE_POR_DEFECTO);
        assertTrue(ProveedorConexion.URL_POR_DEFECTO.contains("eurekabank"));
        assertEquals("com.mysql.cj.jdbc.Driver", ProveedorConexion.DRIVER);
    }
}
