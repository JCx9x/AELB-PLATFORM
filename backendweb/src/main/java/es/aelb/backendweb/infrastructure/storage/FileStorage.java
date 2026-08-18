package es.aelb.backendweb.infrastructure.storage;

import java.io.InputStream;

/** Abstracción del almacenamiento de ficheros binarios fuera de la base de datos. */
public interface FileStorage {

    void put(String key, byte[] content, String contentType);

    InputStream get(String key);

    void delete(String key);
}
