package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdatePaymentStatusUseCase implements UseCase<UpdatePaymentStatusCommand, Void> {

    private final RegistrationRepository registrationRepository;
    private final UserRepository users; private final ChampionshipRepository championships; private final EmailService emails;

    public UpdatePaymentStatusUseCase(RegistrationRepository registrationRepository) { this(registrationRepository,null,null,EmailService.NOOP); }
    public UpdatePaymentStatusUseCase(RegistrationRepository registrationRepository,UserRepository users,ChampionshipRepository championships,EmailService emails) {
        this.registrationRepository = registrationRepository; this.users=users;this.championships=championships;this.emails=emails;
    }

    @Override
    @Transactional
    public Void execute(UpdatePaymentStatusCommand cmd) {
        Registration reg = registrationRepository.findById(cmd.registrationId())
                .orElseThrow(() -> new NotFoundException(cmd.registrationId()));

        switch (cmd.newStatus()) {
            case "PAID"    -> { reg.confirmPayment(); if(users!=null) users.findById(reg.getUserId()).ifPresent(u -> championships.findById(reg.getChampionshipId()).ifPresent(c -> emails.requestPaymentConfirmation(u.getEmail().value(),u.getFullName(),reg.getId(),"Inscripción en "+c.getName(),reg.getAmount().toPlainString()+" EUR"))); }
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
