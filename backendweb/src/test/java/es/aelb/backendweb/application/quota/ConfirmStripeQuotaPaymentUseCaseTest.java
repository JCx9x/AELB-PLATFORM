package es.aelb.backendweb.application.quota;

import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ConfirmStripeQuotaPaymentUseCaseTest {
    @Test
    void confirmaLaCuotaSoloParaLaSesionYElImportePersistidos() {
        QuotaRepository repository = mock(QuotaRepository.class);
        Quota quota = mock(Quota.class);
        when(repository.findByStripeSessionIdForUpdate("cs_quota_123")).thenReturn(Optional.of(quota));
        when(quota.getUserId()).thenReturn(UserId.of("user-1"));
        when(quota.getYear()).thenReturn(2026);
        when(quota.getAmount()).thenReturn(new BigDecimal("25.00"));
        when(quota.isPaid()).thenReturn(false);

        new ConfirmStripeQuotaPaymentUseCase(repository)
                .execute("user-1", 2026, "cs_quota_123", 2500);

        verify(quota).markAsPaidByStripe("cs_quota_123");
        verify(repository).save(quota);
    }

    @Test
    void rechazaMetadataDeOtroUsuario() {
        QuotaRepository repository = mock(QuotaRepository.class);
        Quota quota = mock(Quota.class);
        when(repository.findByStripeSessionIdForUpdate("cs_quota_123")).thenReturn(Optional.of(quota));
        when(quota.getUserId()).thenReturn(UserId.of("user-1"));
        when(quota.getYear()).thenReturn(2026);

        assertThrows(ConfirmStripeQuotaPaymentUseCase.InvalidStripePaymentException.class,
                () -> new ConfirmStripeQuotaPaymentUseCase(repository)
                        .execute("user-2", 2026, "cs_quota_123", 2500));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaImporteManipulado() {
        QuotaRepository repository = mock(QuotaRepository.class);
        Quota quota = mock(Quota.class);
        when(repository.findByStripeSessionIdForUpdate("cs_quota_123")).thenReturn(Optional.of(quota));
        when(quota.getUserId()).thenReturn(UserId.of("user-1"));
        when(quota.getYear()).thenReturn(2026);
        when(quota.getAmount()).thenReturn(new BigDecimal("25.00"));

        assertThrows(ConfirmStripeQuotaPaymentUseCase.InvalidStripePaymentException.class,
                () -> new ConfirmStripeQuotaPaymentUseCase(repository)
                        .execute("user-1", 2026, "cs_quota_123", 1));
        verify(repository, never()).save(any());
    }
}
