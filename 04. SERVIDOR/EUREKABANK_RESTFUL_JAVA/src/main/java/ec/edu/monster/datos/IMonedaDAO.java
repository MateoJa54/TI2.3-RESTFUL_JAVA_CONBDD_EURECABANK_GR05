package ec.edu.monster.datos;

import java.util.Optional;

/**
 * Repositorio de monedas (tabla {@code moneda}).
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface IMonedaDAO {

    /**
     * Nombre oficial de una moneda.
     *
     * @param codigo codigo de la moneda
     * @return el nombre de la moneda, o vacio si no existe
     */
    Optional<String> descripcion(String codigo);

    /**
     * Indica si la moneda esta soportada por el sistema.
     *
     * <p>Una moneda esta soportada cuando existe en el catalogo {@code moneda} y
     * ademas tiene los tres parametros de costo e interes: {@code
     * cargomantenimiento}, {@code costomovimiento} e {@code interesmensual}.
     *
     * @param codigo codigo de la moneda
     * @return {@code true} si la moneda se puede operar
     */
    boolean estaSoportada(String codigo);
}
