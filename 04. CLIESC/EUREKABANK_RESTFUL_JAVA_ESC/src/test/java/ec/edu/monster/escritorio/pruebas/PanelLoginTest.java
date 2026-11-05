package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.cliente.ClienteEUREKABANK;
import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.modelo.RespuestaAutenticacion;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelLogin;
import ec.edu.monster.escritorio.vista.Tema;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Inicio de sesión: validación local, una sola petición, errores del servidor y la clave que nunca se muestra. */
class PanelLoginTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private ControladorAutenticacion controlador;
    private PanelLogin panel;
    private JTextField usuario;
    private JPasswordField clave;
    private JButton boton;
    private JLabel estado;
    private AtomicInteger autenticados;

    @BeforeAll
    static void tema() {
        Swing.enEdt(Tema::instalar);
    }

    @BeforeEach
    void preparar() {
        armar(new ClienteFalso());
    }

    private void armar(ClienteFalso falso) {
        servidor = falso;
        sesion = new SesionUsuario();
        controlador = new ControladorAutenticacion(servidor, sesion);
        autenticados = new AtomicInteger();
        Swing.enEdt(() -> {
            panel = new PanelLogin(controlador);
            panel.setAlAutenticar(autenticados::incrementAndGet);
        });
        usuario = Swing.buscar(panel, "campoUsuario", JTextField.class);
        clave = Swing.buscar(panel, "campoClave", JPasswordField.class);
        boton = Swing.buscar(panel, "botonIngresar", JButton.class);
        estado = Swing.buscar(panel, "etiquetaEstado", JLabel.class);
    }

    private void escribir(String u, String c) {
        Swing.enEdt(() -> {
            usuario.setText(u);
            clave.setText(c);
        });
    }

    private void ingresarYEsperar() {
        Swing.enEdt(panel::ingresar);
        Swing.esperar(() -> boton.isEnabled());
    }

    @Test
    void elBotonDiceIngresarYEsUnoSolo() {
        assertEquals("Ingresar", boton.getText());
        long botones = Swing.todos(panel).stream().filter(c -> c instanceof JButton).count();
        assertEquals(1, botones);
    }

    @Test
    void laEtiquetaDeCadaCampoEstaEncima() {
        var etiquetas = Swing.todos(panel).stream().filter(c -> c instanceof JLabel l
                && ("Usuario".equals(l.getText()) || "Clave".equals(l.getText()))).toList();
        assertEquals(2, etiquetas.size());
        for (var e : etiquetas) {
            JLabel l = (JLabel) e;
            assertTrue(l.getLabelFor() != null, l.getText() + " debe etiquetar un campo");
            assertTrue(l.getY() <= l.getLabelFor().getY() + 1);
        }
    }

    @Test
    void elTituloEsIngresoDeEmpleados() {
        assertTrue(Swing.todos(panel).stream().anyMatch(c -> c instanceof JLabel l && "Ingreso de empleados".equals(l.getText())));
    }

    @Test
    void elEstadoEmpiezaVacio() {
        assertEquals("", estado.getText());
    }

    @Test
    void laClaveEsUnCampoDeContrasenaConEco() {
        assertTrue(clave.getEchoChar() != 0, "la clave debe mostrarse como puntos");
    }

    @Test
    void sinNadaMuestraElAvisoYNoLlamaAlServidor() {
        Swing.enEdt(panel::ingresar);
        assertEquals("Escriba el usuario y la clave.", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertTrue(servidor.llamadas.isEmpty());
        assertTrue(boton.isEnabled());
        assertEquals(0, autenticados.get());
    }

    @ParameterizedTest
    @CsvSource(value = {"'',''", "'  ',''", "'',secreto", "'   ',secreto", "cromero,''", "'\t',secreto"})
    void camposVaciosNoLlamanAlServicio(String u, String c) {
        escribir(u, c);
        Swing.enEdt(panel::ingresar);
        assertEquals("Escriba el usuario y la clave.", estado.getText());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void credencialesCorrectasAbrenLaSesionYAvisan() {
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals(1, autenticados.get());
        assertTrue(sesion.estaVigente());
        assertEquals("", estado.getText());
        assertEquals("Ingresar", boton.getText());
    }

    @Test
    void alServidorSoloLlegaElHashDeLaClave() {
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals(1, servidor.llamadas.size());
        assertEquals("autenticar(cromero," + ClienteFalso.HASH_CLAVE + ")", servidor.llamadas.get(0));
        assertFalse(servidor.llamadas.get(0).contains(ClienteFalso.CLAVE));
    }

    @Test
    void elUsuarioSeRecortaDeEspacios() {
        escribir("  cromero  ", ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals(1, autenticados.get());
        assertTrue(servidor.llamadas.get(0).startsWith("autenticar(cromero,"));
    }

    @Test
    void laClaveSeLimpiaDelCampoAlEnviarse() {
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals(0, clave.getPassword().length);
    }

    @Test
    void laClaveSeLimpiaTambienSiElServidorLaRechaza() {
        escribir(ClienteFalso.USUARIO, "equivocada");
        ingresarYEsperar();
        assertEquals(0, clave.getPassword().length);
    }

    @Test
    void laClaveNuncaApareceEnNingunTextoDeLaVista() {
        escribir(ClienteFalso.USUARIO, "equivocada");
        ingresarYEsperar();
        assertFalse(estado.getText().contains("equivocada"));
        for (var c : Swing.todos(panel)) {
            if (c instanceof JLabel l && l.getText() != null) {
                assertFalse(l.getText().contains("equivocada"));
            }
            if (c instanceof JTextField t && !(c instanceof JPasswordField)) {
                assertFalse(t.getText().contains("equivocada"));
            }
        }
    }

    @Test
    void laClaveCorrectaTampocoApareceEnLaVista() {
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertFalse(estado.getText().contains(ClienteFalso.CLAVE));
        assertFalse(usuario.getText().contains(ClienteFalso.CLAVE));
    }

    @Test
    void credencialesIncorrectasMuestranElMensajeLiteralDelServidor() {
        escribir(ClienteFalso.USUARIO, "equivocada");
        ingresarYEsperar();
        assertEquals("El usuario o la contraseña son incorrectos", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertEquals(0, autenticados.get());
        assertFalse(sesion.estaVigente());
        assertEquals("Ingresar", boton.getText());
    }

    @Test
    void unUsuarioInexistenteMuestraElMismoMensaje() {
        escribir("nadie", "equivocada");
        ingresarYEsperar();
        assertEquals("El usuario o la contraseña son incorrectos", estado.getText());
    }

    @Test
    void despuesDeUnErrorSePuedeIntentarOtraVez() {
        escribir(ClienteFalso.USUARIO, "equivocada");
        ingresarYEsperar();
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals(1, autenticados.get());
        assertEquals("", estado.getText());
        assertEquals(2, servidor.llamadas.size());
    }

    @Test
    void elMensajeDeErrorSeLimpiaAlIntentarOtraVez() throws InterruptedException {
        escribir(ClienteFalso.USUARIO, "equivocada");
        ingresarYEsperar();
        assertFalse(estado.getText().isEmpty());
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals("", estado.getText());
    }

    @Test
    void dobleClicEnviaUnaSolaPeticion() throws InterruptedException {
        CountDownLatch entro = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        ClienteFalso lento = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String u, String c) {
                entro.countDown();
                try {
                    liberar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.autenticar(u, c);
            }
        };
        armar(lento);
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        Swing.enEdt(() -> {
            panel.ingresar();
            panel.ingresar();
            boton.doClick();
        });
        assertTrue(entro.await(5, TimeUnit.SECONDS));
        liberar.countDown();
        Swing.esperar(() -> boton.isEnabled());
        assertEquals(1, lento.llamadas.size());
        assertEquals(1, autenticados.get());
    }

    @Test
    void mientrasProcesaElBotonSeDeshabilitaYDiceProcesando() throws InterruptedException {
        CountDownLatch entro = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        ClienteFalso lento = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String u, String c) {
                entro.countDown();
                try {
                    liberar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.autenticar(u, c);
            }
        };
        armar(lento);
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        Swing.enEdt(panel::ingresar);
        assertTrue(entro.await(5, TimeUnit.SECONDS));
        assertFalse(boton.isEnabled());
        assertEquals("Procesando", boton.getText());
        liberar.countDown();
        Swing.esperar(() -> boton.isEnabled());
        assertEquals("Ingresar", boton.getText());
    }

    @Test
    void elHiloDeEventosNoSeBloqueaMientrasElServidorTarda() throws InterruptedException {
        CountDownLatch entro = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        ClienteFalso lento = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String u, String c) {
                entro.countDown();
                try {
                    liberar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.autenticar(u, c);
            }
        };
        armar(lento);
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        Swing.enEdt(panel::ingresar);
        assertTrue(entro.await(5, TimeUnit.SECONDS));
        long inicio = System.nanoTime();
        Swing.enEdt(() -> { });
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        liberar.countDown();
        assertTrue(ms < 1_000, "el hilo de eventos tardó " + ms + " ms en atender");
        Swing.esperar(() -> boton.isEnabled());
    }

    @Test
    void servidorApagadoMuestraSer001YLaAplicacionSigueViva() {
        servidor.servidorApagado = true;
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals("El servicio no está disponible en este momento", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertEquals("Ingresar", boton.getText());
        assertEquals(0, autenticados.get());
        servidor.servidorApagado = false;
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals(1, autenticados.get());
    }

    @Test
    void servidorApagadoNoRevelaLaDireccionInterna() {
        servidor.servidorApagado = true;
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertFalse(estado.getText().contains("http"));
    }

    @Test
    void unErrorInesperadoSeMuestraComoErrorInterno() {
        ClienteFalso roto = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String u, String c) {
                throw new IllegalStateException("fallo interno con datos sensibles");
            }
        };
        armar(roto);
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals("Ocurrió un error interno, intente nuevamente", estado.getText());
        assertFalse(estado.getText().contains("sensibles"));
    }

    @Test
    void unaRespuestaNulaSeMuestraComoErrorInterno() {
        ClienteFalso nulo = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String u, String c) {
                return null;
            }
        };
        armar(nulo);
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals("Ocurrió un error interno, intente nuevamente", estado.getText());
        assertEquals(0, autenticados.get());
    }

    @Test
    void unaRespuestaOkSinTokenNoAbreLaSesion() {
        ClienteFalso sinToken = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String u, String c) {
                RespuestaAutenticacion r = new RespuestaAutenticacion();
                r.setEstado("OK");
                r.setCodigo("200");
                r.setMensaje("Autenticación exitosa");
                return r;
            }
        };
        armar(sinToken);
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertEquals(0, autenticados.get());
        assertFalse(sesion.estaVigente());
    }

    @Test
    void enterEnLaClaveTambienIngresa() {
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        Swing.enEdt(clave::postActionEvent);
        Swing.esperar(() -> boton.isEnabled());
        assertEquals(1, autenticados.get());
    }

    @Test
    void sinManejadorDeExitoNoFalla() {
        Swing.enEdt(() -> panel.setAlAutenticar(null));
        escribir(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        ingresarYEsperar();
        assertTrue(sesion.estaVigente());
    }

    @Test
    void mostrarEstadoPintaErrorYExito() {
        Swing.enEdt(() -> panel.mostrarEstado("Algo salió mal", true));
        assertEquals("Algo salió mal", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
        Swing.enEdt(() -> panel.mostrarEstado("Todo bien", false));
        assertEquals(Tema.EXITO, estado.getForeground());
        Swing.enEdt(() -> panel.mostrarEstado(null, false));
        assertEquals("", estado.getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ñandú", "contraseña con espacios", "a'b\"c", "<script>", "日本語"})
    void claveConCaracteresRarosSeEnviaComoHash(String rara) {
        escribir(ClienteFalso.USUARIO, rara);
        ingresarYEsperar();
        assertEquals(1, servidor.llamadas.size());
        assertFalse(servidor.llamadas.get(0).contains(rara));
        assertNotEquals("", estado.getText());
    }

    @Test
    void dosIngresosSeguidosHacenDosPeticiones() {
        escribir(ClienteFalso.USUARIO, "equivocada");
        ingresarYEsperar();
        escribir(ClienteFalso.USUARIO, "equivocada");
        ingresarYEsperar();
        assertEquals(2, servidor.llamadas.size());
    }

    @Test
    void conUnServidorRealQueDevuelveHtmlMuestraSer001() throws Exception {
        try (ServidorDePrueba http = new ServidorDePrueba()) {
            http.registrar("POST", "/EUREKABANK_RESTFUL_JAVA/api/auth/login", 200, "text/html; charset=utf-8",
                    "<html><body>No soy JSON</body></html>");
            SesionUsuario s = new SesionUsuario();
            ControladorAutenticacion c = new ControladorAutenticacion(
                    new ClienteEUREKABANK(http.url("/EUREKABANK_RESTFUL_JAVA/api")), s);
            Swing.enEdt(() -> {
                panel = new PanelLogin(c);
            });
            usuario = Swing.buscar(panel, "campoUsuario", JTextField.class);
            clave = Swing.buscar(panel, "campoClave", JPasswordField.class);
            boton = Swing.buscar(panel, "botonIngresar", JButton.class);
            estado = Swing.buscar(panel, "etiquetaEstado", JLabel.class);
            escribir("cromero", "Cromero2026");
            ingresarYEsperar();
            assertEquals("El servicio no está disponible en este momento", estado.getText());
            assertFalse(s.estaVigente());
        }
    }

    @Test
    void conUnServidorRealCaidoMuestraSer001() throws Exception {
        String url;
        try (ServidorDePrueba http = new ServidorDePrueba()) {
            url = http.url("/EUREKABANK_RESTFUL_JAVA/api");
        }
        SesionUsuario s = new SesionUsuario();
        ControladorAutenticacion c = new ControladorAutenticacion(new ClienteEUREKABANK(url), s);
        Swing.enEdt(() -> {
            panel = new PanelLogin(c);
        });
        usuario = Swing.buscar(panel, "campoUsuario", JTextField.class);
        clave = Swing.buscar(panel, "campoClave", JPasswordField.class);
        boton = Swing.buscar(panel, "botonIngresar", JButton.class);
        estado = Swing.buscar(panel, "etiquetaEstado", JLabel.class);
        escribir("cromero", "Cromero2026");
        ingresarYEsperar();
        assertEquals("El servicio no está disponible en este momento", estado.getText());
        assertEquals("Ingresar", boton.getText());
    }
}
