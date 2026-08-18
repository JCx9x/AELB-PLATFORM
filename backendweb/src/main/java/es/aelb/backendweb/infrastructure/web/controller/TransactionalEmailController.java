package es.aelb.backendweb.infrastructure.web.controller;
import es.aelb.backendweb.application.user.*; import es.aelb.backendweb.infrastructure.web.dto.request.*; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class TransactionalEmailController {
 private final RequestPasswordResetUseCase requestReset; private final ResetPasswordUseCase reset; private final VerifyEmailUseCase verify; private final ResendEmailVerificationUseCase resend;
 public TransactionalEmailController(RequestPasswordResetUseCase requestReset,ResetPasswordUseCase reset,VerifyEmailUseCase verify,ResendEmailVerificationUseCase resend){this.requestReset=requestReset;this.reset=reset;this.verify=verify;this.resend=resend;}
 @PostMapping("/password/forgot") public ResponseEntity<Map<String,String>> forgot(@Valid @RequestBody ForgotPasswordRequest r){requestReset.execute(r.email());return ResponseEntity.ok(Map.of("message","Si existe una cuenta asociada a ese correo, recibirás las instrucciones."));}
 @PostMapping("/password/reset") public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest r){reset.execute(r.token(),r.password());return ResponseEntity.noContent().build();}
 @PostMapping("/email/verify") public ResponseEntity<Void> verify(@Valid @RequestBody VerificationTokenRequest r){verify.execute(r.token());return ResponseEntity.noContent().build();}
 @PostMapping("/email/resend-verification") public ResponseEntity<Map<String,String>> resend(Authentication a){if(a!=null&&a.getPrincipal()!=null)resend.execute((String)a.getPrincipal());return ResponseEntity.ok(Map.of("message","Si la cuenta puede verificarse, recibirás un email."));}
}
