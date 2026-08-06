package es.aelb.backendweb.infrastructure.web.dto.response;
import es.aelb.backendweb.domain.quota.Quota;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record QuotaResponse(String id, int year, String ageCategory, String ageCategoryLabel, BigDecimal amount, String paymentStatus, LocalDate paymentDate, LocalDateTime paidAt, String paymentSource, String paidByUserId) {
    public static QuotaResponse from(Quota q) { return new QuotaResponse(q.getId(), q.getYear(), q.getAgeCategory().name(), q.getAgeCategory().getLabel(), q.getAmount(), q.getStatus().name(), q.getPaymentDate(), q.getPaidAt(), q.getPaymentSource() == null ? null : q.getPaymentSource().name(), q.getPaidByUserId() == null ? null : q.getPaidByUserId().value()); }
}
