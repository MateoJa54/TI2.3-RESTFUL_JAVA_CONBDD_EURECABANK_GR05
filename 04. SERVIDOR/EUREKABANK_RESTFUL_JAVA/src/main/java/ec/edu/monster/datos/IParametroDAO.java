package ec.edu.monster.datos;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repositorio de parametros de negocio (tabla {@code parametro}).
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface IParametroDAO {

    /**
     * Lee el valor de un parametro de negocio.
     *
     * @param codigo codigo del parametro ({@code 001} es el ITF y {@code 002} el
     *               numero de operaciones sin costo)
     * @return el valor numerico, o vacio si el parametro no existe
     */
    Optional<BigDecimal> valor(String codigo);
}
