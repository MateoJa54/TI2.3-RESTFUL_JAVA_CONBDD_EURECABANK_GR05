package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.monster.api.rest.ServiciosEurekaBank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Lectura de EUREKABANK_TOKEN_MINUTOS (contrato, seccion 11.1). */
class ServiciosEurekaBankTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "-3", "abc", "1.5"})
    void valorAusenteOInvalidoUsaLos30Minutos(String valor) {
        assertEquals(30, ServiciosEurekaBank.leerMinutos(valor));
    }

    @Test
    void unEnteroPositivoSeUsaComoMinutos() {
        assertEquals(15, ServiciosEurekaBank.leerMinutos("15"));
    }

    @Test
    void losEspaciosAlrededorDelEnteroSeIgnoran() {
        assertEquals(20, ServiciosEurekaBank.leerMinutos(" 20 "));
    }
}
