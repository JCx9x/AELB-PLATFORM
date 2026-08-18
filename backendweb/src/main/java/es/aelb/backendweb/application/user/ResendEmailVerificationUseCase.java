package es.aelb.backendweb.application.user;
import es.aelb.backendweb.application.email.EmailService; import es.aelb.backendweb.domain.user.UserRepository; import es.aelb.backendweb.domain.user.valueobject.UserId;
public class ResendEmailVerificationUseCase { private final UserRepository users; private final EmailService emails; public ResendEmailVerificationUseCase(UserRepository users,EmailService emails){this.users=users;this.emails=emails;} public void execute(String userId){users.findById(UserId.of(userId)).ifPresent(emails::requestEmailVerification);} }
