package es.aelb.backendweb.infrastructure.web.controller;
import es.aelb.backendweb.application.user.*; import es.aelb.backendweb.infrastructure.web.dto.request.*; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.authentication.AnonymousAuthenticationToken; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class TransactionalEmailController {
 private final RequestPasswordResetUseCase requestReset; private final ResetPasswordUseCase reset; private final VerifyEmailUseCase verify; private final ResendEmailVerificationUseCase resend; private final ResendEmailVerificationByEmailUseCase resendByEmail; private final VerifyRecoveryEmailUseCase verifyRecovery;
 public TransactionalEmailController(RequestPasswordResetUseCase requestReset,ResetPasswordUseCase reset,VerifyEmailUseCase verify,ResendEmailVerificationUseCase resend,ResendEmailVerificationByEmailUseCase resendByEmail,VerifyRecoveryEmailUseCase verifyRecovery){this.requestReset=requestReset;this.reset=reset;this.verify=verify;this.resend=resend;this.resendByEmail=resendByEmail;this.verifyRecovery=verifyRecovery;}
 // Misma respuesta exista o no la cuenta (o el correo de recuperación): no revela qué correos están registrados.
 @PostMapping("/password/forgot") public ResponseEntity<Map<String,String>> forgot(@Valid @RequestBody ForgotPasswordRequest r){if(r.useRecoveryEmail())requestReset.executeViaRecoveryEmail(r.email());else requestReset.execute(r.email());return ResponseEntity.ok(Map.of("message","Si existe una cuenta asociada a ese correo, recibirás las instrucciones."));}
 @PostMapping("/password/reset") public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest r){reset.execute(r.token(),r.password());return ResponseEntity.noContent().build();}
 @PostMapping("/recovery-email/verify") public ResponseEntity<Void> verifyRecoveryEmail(@Valid @RequestBody VerificationTokenRequest r){verifyRecovery.execute(r.token());return ResponseEntity.noContent().build();}
 @PostMapping("/email/verify") public ResponseEntity<Void> verify(@Valid @RequestBody VerificationTokenRequest r){verify.execute(r.token());return ResponseEntity.noContent().build();}
 // Anónimo por diseño: una cuenta sin verificar no puede autenticarse (ver LoginUseCase),
 // así que necesita una vía de reenvío que no dependa de tener sesión. Si llega un
 // Authentication real (no el token anónimo de Spring Security) se usa esa sesión en vez
 // del email del cuerpo, igual que antes de este cambio.
 @PostMapping("/email/resend-verification") public ResponseEntity<Map<String,String>> resend(Authentication a,@Valid @RequestBody(required=false) ResendVerificationRequest r){if(a!=null&&!(a instanceof AnonymousAuthenticationToken)&&a.getPrincipal()!=null)resend.execute((String)a.getPrincipal());else if(r!=null)resendByEmail.execute(r.email());return ResponseEntity.ok(Map.of("message","Si la cuenta puede verificarse, recibirás un email."));}
}
