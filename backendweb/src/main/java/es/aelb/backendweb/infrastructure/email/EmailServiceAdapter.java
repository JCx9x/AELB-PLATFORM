package es.aelb.backendweb.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.aelb.backendweb.application.email.*;
import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.infrastructure.config.EmailProperties;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.*;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets; import java.security.*; import java.time.*; import java.util.*;

@Service
public class EmailServiceAdapter implements EmailService {
 private final UserRepository users; private final SpringEmailOutboxJpaRepository outbox; private final SpringPasswordResetTokenJpaRepository resets; private final SpringEmailVerificationTokenJpaRepository verifications; private final EmailProperties props; private final EmailPayloadCrypto crypto; private final ObjectMapper mapper; private final SecureRandom random = new SecureRandom();
 private final SpringUserJpaRepository lockedUsers;
 public EmailServiceAdapter(UserRepository users, SpringEmailOutboxJpaRepository outbox, SpringPasswordResetTokenJpaRepository resets, SpringEmailVerificationTokenJpaRepository verifications, EmailProperties props, EmailPayloadCrypto crypto, ObjectMapper mapper, SpringUserJpaRepository lockedUsers) { this.users=users;this.outbox=outbox;this.resets=resets;this.verifications=verifications;this.props=props;this.crypto=crypto;this.mapper=mapper;this.lockedUsers=lockedUsers; }
 @Override @Transactional public void requestEmailVerification(User user) {
   if (!props.isEnabled()) return;
   if (user.isEmailVerified()) return; String id=user.getId().value(); lockedUsers.findByIdForUpdate(id); LocalDateTime now=LocalDateTime.now();
   if (verifications.existsByUserIdAndCreatedAtAfter(id, now.minusSeconds(props.getEmailVerificationCooldownSeconds()))) return;
   if (verifications.countByUserIdAndCreatedAtAfter(id, now.minusHours(1)) >= props.getEmailVerificationMaxPerHour()) return;
   verifications.deleteByUserId(id); String token=token(); EmailVerificationTokenJpaEntity t=new EmailVerificationTokenJpaEntity(); t.setUserId(id);t.setTokenHash(hash(token));t.setExpiresAt(now.plusHours(props.getEmailVerificationTokenExpirationHours()));t.setCreatedAt(now);t.setUsed(false);verifications.save(t);
   enqueue(EmailType.EMAIL_VERIFICATION,user.getEmail().value(),"EMAIL_VERIFICATION:"+id+":"+now.toEpochSecond(ZoneOffset.UTC), tokenPayload(token,user.getFirstName(),"verify-email"));
 }
 @Override @Transactional public void requestPasswordReset(String email) {
   if (!props.isEnabled()) return;
   Optional<User> found=users.findByEmail(Email.of(email)); if (found.isEmpty()) { random.nextBytes(new byte[32]); return; }
   User user=found.get(); String id=user.getId().value(); lockedUsers.findByIdForUpdate(id); LocalDateTime now=LocalDateTime.now();
   if (resets.existsByUserIdAndCreatedAtAfter(id, now.minusSeconds(props.getPasswordResetCooldownSeconds())) || resets.countByUserIdAndCreatedAtAfter(id, now.minusHours(1)) >= props.getPasswordResetMaxPerHour()) return;
   resets.deleteByUserId(id); String token=token(); PasswordResetTokenJpaEntity t=new PasswordResetTokenJpaEntity();t.setUserId(id);t.setTokenHash(hash(token));t.setExpiresAt(now.plusMinutes(props.getPasswordResetTokenExpirationMinutes()));t.setCreatedAt(now);t.setUsed(false);resets.save(t);
   enqueue(EmailType.PASSWORD_RESET,user.getEmail().value(),"PASSWORD_RESET:"+id+":"+now.toEpochSecond(ZoneOffset.UTC), tokenPayload(token,user.getFirstName(),"reset-password"));
 }
 @Override @Transactional public void requestRegistrationConfirmation(String recipient,String name,String registrationId,String championshipName) { if (!props.isEnabled()) return; enqueue(EmailType.REGISTRATION_CONFIRMATION,recipient,"REGISTRATION_CONFIRMATION:"+registrationId,payload(Map.of("name",name,"championship",championshipName))); }
 @Override @Transactional public void requestPaymentConfirmation(String recipient,String name,String paymentId,String description,String amount) { if (!props.isEnabled()) return; enqueue(EmailType.PAYMENT_CONFIRMATION,recipient,"PAYMENT_CONFIRMATION:"+paymentId,payload(Map.of("name",name,"description",description,"amount",amount))); }
 private void enqueue(EmailType type,String recipient,String key,String payload) { LocalDateTime now=LocalDateTime.now(); outbox.insertIgnore(UUID.randomUUID().toString(),type.name(),recipient,crypto.encrypt(payload),now,key); }
 private String token(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private String tokenPayload(String token,String name,String path){return payload(Map.of("token",token,"name",name,"path",path));}
 private String payload(Map<String,String> v){try{return mapper.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException(e);}}
}
