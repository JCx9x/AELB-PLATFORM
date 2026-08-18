package es.aelb.backendweb.infrastructure.persistence.jpa.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name="email_verification_tokens", uniqueConstraints=@UniqueConstraint(name="uq_email_verification_hash", columnNames="token_hash"))
@Getter @Setter @NoArgsConstructor
public class EmailVerificationTokenJpaEntity {
 @Id @Column(length=36) private String id=UUID.randomUUID().toString(); @Column(name="user_id",nullable=false,length=36) private String userId;
 @Column(name="token_hash",nullable=false,length=64) private String tokenHash; @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
 @Column(nullable=false) private boolean used; @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
