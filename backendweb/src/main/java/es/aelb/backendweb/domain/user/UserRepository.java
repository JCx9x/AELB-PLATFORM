package es.aelb.backendweb.domain.user;

import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (driven port).
 * Define el contrato que la infraestructura debe cumplir.
 * Sin ninguna dependencia de framework o base de datos.
 */
public interface UserRepository {

    void   save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    Optional<User> findByDni(Dni dni);
    boolean        existsByEmail(Email email);
    boolean        existsByDni(Dni dni);
    List<User>     findAll();
    List<User>     findByTeamId(String teamId);
    void           delete(UserId id);
}
