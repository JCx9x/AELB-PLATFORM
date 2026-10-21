package es.aelb.backendweb.infrastructure.email;

import es.aelb.backendweb.domain.shared.pagination.PageResult;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.config.EmailProperties;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;

class EmailServiceAdapterTest {
    @Test void emailDeshabilitadoNoEncolaVerificacion() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UserId.of("u1"));
        when(user.getEmail()).thenReturn(Email.of("user@example.com"));
        EmailProperties p = new EmailProperties(); p.setEnabled(false);
        SpringEmailOutboxJpaRepository outbox = mock(SpringEmailOutboxJpaRepository.class);
        SpringEmailVerificationTokenJpaRepository verifications = mock(SpringEmailVerificationTokenJpaRepository.class);

        EmailServiceAdapter service = new EmailServiceAdapter(
                new UserRepositoryAdapterStub(user), outbox, mock(SpringPasswordResetTokenJpaRepository.class),
                verifications, p, new EmailPayloadCrypto(p), new ObjectMapper(), mock(SpringUserJpaRepository.class));
        service.requestEmailVerification(user);

        verifyNoInteractions(outbox, verifications);
    }

    @Test void emailHabilitadoEncolaVerificacion() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UserId.of("u1"));
        when(user.getEmail()).thenReturn(Email.of("user@example.com"));
        when(user.getFirstName()).thenReturn("Test");
        when(user.isEmailVerified()).thenReturn(false);

        SpringEmailVerificationTokenJpaRepository verifications = mock(SpringEmailVerificationTokenJpaRepository.class);
        when(verifications.existsByUserIdAndCreatedAtAfter(anyString(), any())).thenReturn(false);
        when(verifications.countByUserIdAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        SpringEmailOutboxJpaRepository outbox = mock(SpringEmailOutboxJpaRepository.class);
        EmailProperties p = new EmailProperties(); p.setEnabled(true); p.setTokenEncryptionKey("test");

        EmailServiceAdapter service = new EmailServiceAdapter(
                new UserRepositoryAdapterStub(user), outbox, mock(SpringPasswordResetTokenJpaRepository.class),
                verifications, p, new EmailPayloadCrypto(p), new ObjectMapper(), mock(SpringUserJpaRepository.class));
        service.requestEmailVerification(user);

        verify(outbox).insertIgnore(anyString(), eq("EMAIL_VERIFICATION"), eq("user@example.com"), anyString(), any(), contains("EMAIL_VERIFICATION:u1:"));
    }

    @Test void segundoForgotDuranteCooldownNoCreaOtroOutbox() {
        User user = mock(User.class); when(user.getId()).thenReturn(UserId.of("u1")); when(user.getEmail()).thenReturn(Email.of("user@example.com")); when(user.getFirstName()).thenReturn("Test");
        UserRepositoryAdapterStub users = new UserRepositoryAdapterStub(user);
        SpringPasswordResetTokenJpaRepository resets = mock(SpringPasswordResetTokenJpaRepository.class); when(resets.existsByUserIdAndCreatedAtAfter(anyString(), any())).thenReturn(true);
        SpringEmailOutboxJpaRepository outbox = mock(SpringEmailOutboxJpaRepository.class);
        EmailProperties p = new EmailProperties(); p.setTokenEncryptionKey("test");
        EmailServiceAdapter service = new EmailServiceAdapter(users, outbox, resets, mock(SpringEmailVerificationTokenJpaRepository.class), p, new EmailPayloadCrypto(p), new ObjectMapper(), mock(SpringUserJpaRepository.class));
        service.requestPasswordReset("user@example.com");
        verify(outbox, never()).insertIgnore(anyString(), anyString(), anyString(), anyString(), any(), anyString());
    }
    private static final class UserRepositoryAdapterStub implements es.aelb.backendweb.domain.user.UserRepository { private final User user; UserRepositoryAdapterStub(User user){this.user=user;} public void save(User u){} public Optional<User> findById(UserId id){return Optional.of(user);} public Optional<User> findByEmail(Email e){return Optional.of(user);} public Optional<User> findByDni(es.aelb.backendweb.domain.user.valueobject.Dni d){return Optional.empty();} public boolean existsByEmail(Email e){return true;} public boolean existsByDni(es.aelb.backendweb.domain.user.valueobject.Dni d){return false;} public java.util.List<User> findAll(){return java.util.List.of(user);} public PageResult<User> findPage(int page,int size){return new PageResult<>(java.util.List.of(),page,size,0,0);} public PageResult<User> findPage(int page,int size,String search,String role){return new PageResult<>(java.util.List.of(),page,size,0,0);} public java.util.List<User> findByTeamId(String id){return java.util.List.of();} public void delete(UserId id){} }
}
