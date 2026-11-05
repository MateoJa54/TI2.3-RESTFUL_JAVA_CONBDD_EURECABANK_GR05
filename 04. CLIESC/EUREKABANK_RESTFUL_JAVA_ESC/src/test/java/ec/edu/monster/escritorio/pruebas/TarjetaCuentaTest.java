package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.modelo.CuentaDTO;
import ec.edu.monster.escritorio.vista.Tema;
import ec.edu.monster.escritorio.vista.TarjetaCuenta;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tarjeta de cuenta: datos, chips, saldo y las tres acciones. */
class TarjetaCuentaTest {

    @BeforeAll
    static void tema() {
        Swing.enEdt(Tema::instalar);
    }

    private static CuentaDTO cuenta(String codigo, String estado) {
        CuentaDTO cuenta = new CuentaDTO();
        cuenta.setCodigo(codigo);
        cuenta.setDescripcionMoneda("Soles");
        cuenta.setSaldo(new BigDecimal("6800"));
        cuenta.setEstado(estado);
        cuenta.setOperacionesRealizadas(7);
        return cuenta;
    }

    @Test
    void muestraCodigoClienteChipsSaldoYOperaciones() {
        TarjetaCuenta tarjeta = new TarjetaCuenta("Carlos Romero", cuenta("00200002", "ACTIVO"), null);
        JLabel codigo = Swing.buscar(tarjeta, "codigo", JLabel.class);
        assertEquals("00200002", codigo.getText());
        assertEquals(Font.MONOSPACED, codigo.getFont().getName());
        assertEquals("Carlos Romero", Swing.buscar(tarjeta, "cliente", JLabel.class).getText());
        assertEquals("Soles", Swing.buscar(tarjeta, "moneda", JLabel.class).getText());
        assertEquals("6,800.00", Swing.buscar(tarjeta, "saldo", JLabel.class).getText());
        assertEquals(32f, Swing.buscar(tarjeta, "saldo", JLabel.class).getFont().getSize2D(), 0.001f);
        assertEquals("7 operaciones", Swing.buscar(tarjeta, "operaciones", JLabel.class).getText());
    }

    @Test
    void elEstadoActivoVaEnVerdeYLosDemasEnRojo() {
        JLabel activo = Swing.buscar(new TarjetaCuenta("A", cuenta("1", "ACTIVO"), null), "estado", JLabel.class);
        assertEquals("ACTIVO", activo.getText());
        assertEquals(Tema.EXITO, activo.getForeground());

        JLabel bloqueado = Swing.buscar(new TarjetaCuenta("A", cuenta("1", "BLOQUEADO"), null), "estado", JLabel.class);
        assertEquals("BLOQUEADO", bloqueado.getText());
        assertEquals(Tema.ERROR, bloqueado.getForeground());
    }

    @Test
    void lasTresAccionesAvisanConSuDestinoYElCodigo() {
        List<String> llamadas = new ArrayList<>();
        TarjetaCuenta tarjeta = new TarjetaCuenta("A", cuenta("00200002", "ACTIVO"),
                (accion, codigo) -> llamadas.add(accion + " " + codigo));
        Swing.enEdt(() -> {
            Swing.buscar(tarjeta, "accionMovimientos", JButton.class).doClick();
            Swing.buscar(tarjeta, "accionCostos", JButton.class).doClick();
            Swing.buscar(tarjeta, "accionOperar", JButton.class).doClick();
        });
        assertEquals(List.of("MOVIMIENTOS 00200002", "COSTOS 00200002", "OPERAR 00200002"), llamadas);
    }

    @Test
    void textosDeLasAccionesSonLosDelDiseno() {
        TarjetaCuenta tarjeta = new TarjetaCuenta("A", cuenta("1", "ACTIVO"), null);
        assertEquals("Ver movimientos", Swing.buscar(tarjeta, "accionMovimientos", JButton.class).getText());
        assertEquals("Ver costos", Swing.buscar(tarjeta, "accionCostos", JButton.class).getText());
        assertEquals("Operar", Swing.buscar(tarjeta, "accionOperar", JButton.class).getText());
    }

    @Test
    void sinCuentaNoSeConstruye() {
        assertThrows(IllegalArgumentException.class, () -> new TarjetaCuenta("A", null, null));
    }

    @Test
    void conSaldoNuloMuestraCeroConDosDecimales() {
        CuentaDTO sinSaldo = cuenta("1", "ACTIVO");
        sinSaldo.setSaldo(null);
        assertTrue(Swing.buscar(new TarjetaCuenta("A", sinSaldo, null), "saldo", JLabel.class)
                .getText().endsWith("0.00"));
    }
}
