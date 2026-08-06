package es.aelb.backendweb.application.shared;

/**
 * Contrato base para casos de uso.
 * INPUT  → comando o query de entrada.
 * OUTPUT → resultado de la operación (Void para comandos sin respuesta).
 */
public interface UseCase<INPUT, OUTPUT> {
    OUTPUT execute(INPUT input);
}
