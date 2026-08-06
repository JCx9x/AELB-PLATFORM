package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.shared.exception.DomainException;

public class UpdatePaymentStatusUseCase implements UseCase<UpdatePaymentStatusCommand, Void> {

    private final RegistrationRepository registrationRepository;

    public UpdatePaymentStatusUseCase(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    public Void execute(UpdatePaymentStatusCommand cmd) {
        Registration reg = registrationRepository.findById(cmd.registrationId())
                .orElseThrow(() -> new NotFoundException(cmd.registrationId()));

        switch (cmd.newStatus()) {
            case "PAID"    -> reg.confirmPayment();
            case "PENDING" -> reg.resetToPending();
            default        -> throw new InvalidStatusException(cmd.newStatus());
        }

        registrationRepository.save(reg);
        return null;
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Inscripción no encontrada: " + id);
        }
    }

    public static final class InvalidStatusException extends DomainException {
        public InvalidStatusException(String status) {
            super("Estado de pago no válido: " + status + ". Valores permitidos: PENDING, PAID");
        }
    }
}
