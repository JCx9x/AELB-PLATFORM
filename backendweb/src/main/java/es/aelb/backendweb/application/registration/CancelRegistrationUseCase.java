package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.shared.exception.DomainException;

public class CancelRegistrationUseCase implements UseCase<CancelRegistrationCommand, Void> {

    private final RegistrationRepository registrationRepository;

    public CancelRegistrationUseCase(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    public Void execute(CancelRegistrationCommand cmd) {
        Registration reg = registrationRepository.findById(cmd.registrationId())
                .orElseThrow(() -> new NotFoundException(cmd.registrationId()));

        throw new CancellationNotAllowedException(reg.getId());
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Inscripción no encontrada: " + id);
        }
    }

    public static final class ForbiddenOperationException extends DomainException {
        public ForbiddenOperationException(String id) {
            super("No tienes permiso para cancelar la inscripción: " + id);
        }
    }

    public static final class CancellationNotAllowedException extends DomainException {
        public CancellationNotAllowedException(String id) {
            super("No se puede cancelar una inscripción ya realizada: " + id);
        }
    }
}
