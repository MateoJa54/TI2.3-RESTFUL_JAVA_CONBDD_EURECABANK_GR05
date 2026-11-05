package ec.edu.monster.escritorio.pruebas;

import ec.edu.monster.escritorio.Main;

/** Imprime la dirección que resuelve Main; se ejecuta en otro proceso para probar la variable de entorno. */
public final class ResolverUrlProceso {

    private ResolverUrlProceso() {
    }

    /**
     * @param argumentos argumentos que recibiría Main
     */
    public static void main(String[] argumentos) {
        System.out.println(Main.resolverUrl(argumentos));
    }
}
