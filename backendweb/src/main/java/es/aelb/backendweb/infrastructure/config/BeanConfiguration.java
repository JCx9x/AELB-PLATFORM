package es.aelb.backendweb.infrastructure.config;

import es.aelb.backendweb.application.pricing.*;
import es.aelb.backendweb.application.document.CreateDocumentUseCase;
import es.aelb.backendweb.application.document.DeleteDocumentUseCase;
import es.aelb.backendweb.application.document.UpdateDocumentUseCase;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.application.team.CreateTeamUseCase;
import es.aelb.backendweb.application.team.DeleteTeamUseCase;
import es.aelb.backendweb.application.team.UpdateTeamUseCase;
import es.aelb.backendweb.domain.team.TeamRepository;
import es.aelb.backendweb.domain.pricing.PricingConfigRepository;
import es.aelb.backendweb.domain.pricing.ChampionshipAgeCategoryPriceRepository;
import es.aelb.backendweb.domain.pricing.ChampionshipRegistrationPriceCalculator;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.pricing.engine.PricingEngine;
import es.aelb.backendweb.application.category.CreateCategoryUseCase;
import es.aelb.backendweb.application.category.DeleteCategoryUseCase;
import es.aelb.backendweb.application.category.UpdateCategoryUseCase;
import es.aelb.backendweb.application.championship.CreateChampionshipUseCase;
import es.aelb.backendweb.application.news.CreateNewsUseCase;
import es.aelb.backendweb.application.news.DeleteNewsUseCase;
import es.aelb.backendweb.application.news.UpdateNewsUseCase;
import es.aelb.backendweb.application.result.CreateResultUseCase;
import es.aelb.backendweb.application.result.DeleteResultUseCase;
import es.aelb.backendweb.application.result.UpdateResultUseCase;
import es.aelb.backendweb.domain.news.NewsRepository;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.application.championship.DeleteChampionshipUseCase;
import es.aelb.backendweb.application.championship.UpdateChampionshipUseCase;
import es.aelb.backendweb.application.registration.CancelRegistrationUseCase;
import es.aelb.backendweb.application.registration.RegisterForChampionshipUseCase;
import es.aelb.backendweb.application.registration.UpdatePaymentStatusUseCase;
import es.aelb.backendweb.application.registration.ChangeRegistrationCategoryUseCase;
import es.aelb.backendweb.application.registration.CreateStripeRegistrationCheckoutUseCase;
import es.aelb.backendweb.application.registration.ConfirmStripeRegistrationPaymentUseCase;
import es.aelb.backendweb.application.registration.CreateStripeChampionshipBasketCheckoutUseCase;
import es.aelb.backendweb.application.registration.ConfirmStripeChampionshipBasketPaymentUseCase;
import es.aelb.backendweb.application.auth.IssueRefreshTokenUseCase;
import es.aelb.backendweb.application.auth.LogoutUseCase;
import es.aelb.backendweb.application.auth.RefreshSessionUseCase;
import es.aelb.backendweb.application.user.AdminUpdateUserUseCase;
import es.aelb.backendweb.application.user.ChangePasswordUseCase;
import es.aelb.backendweb.application.user.CreateUserUseCase;
import es.aelb.backendweb.application.user.DeleteUserUseCase;
import es.aelb.backendweb.application.user.LoginUseCase;
import es.aelb.backendweb.application.user.ManagementUpdateProfileUseCase;
import es.aelb.backendweb.application.user.UpdateProfileUseCase;
import es.aelb.backendweb.application.user.RequestPasswordResetUseCase;
import es.aelb.backendweb.application.user.ResetPasswordUseCase;
import es.aelb.backendweb.application.user.VerifyEmailUseCase;
import es.aelb.backendweb.application.user.ResendEmailVerificationUseCase;
import es.aelb.backendweb.application.email.EmailService;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringPasswordResetTokenJpaRepository;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringEmailVerificationTokenJpaRepository;
import es.aelb.backendweb.domain.auth.RefreshTokenRepository;
import es.aelb.backendweb.domain.category.CategoryRepository;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.quota.QuotaRepository;
import es.aelb.backendweb.domain.quota.AnnualQuotaPriceRepository;
import es.aelb.backendweb.application.quota.GetOrCreateUserQuotaUseCase;
import es.aelb.backendweb.application.quota.MarkQuotaPaidUseCase;
import es.aelb.backendweb.application.quota.SaveAnnualQuotaPricesUseCase;
import es.aelb.backendweb.application.quota.CreateStripeQuotaCheckoutUseCase;
import es.aelb.backendweb.application.quota.ConfirmStripeQuotaPaymentUseCase;
import es.aelb.backendweb.domain.quota.StripeCheckoutGateway;
import es.aelb.backendweb.infrastructure.payment.StripeCheckoutGatewayAdapter;
import es.aelb.backendweb.infrastructure.payment.StripeWebhookVerifier;
import es.aelb.backendweb.infrastructure.storage.FileStorage;
import es.aelb.backendweb.infrastructure.config.StripeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

/**
 * Cableado de dependencias (Composition Root).
 * Aquí se ensamblan los casos de uso con sus puertos.
 * Los casos de uso son POJOs: no tienen @Service ni @Component.
 */
@Configuration
public class BeanConfiguration {

    /** Jackson no se autoconfigura en esta combinación de starters de Spring Boot 4. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public SaveAnnualQuotaPricesUseCase saveAnnualQuotaPricesUseCase(AnnualQuotaPriceRepository repository) { return new SaveAnnualQuotaPricesUseCase(repository); }

    @Bean
    public GetOrCreateUserQuotaUseCase getOrCreateUserQuotaUseCase(QuotaRepository quotas, AnnualQuotaPriceRepository prices, UserRepository users) { return new GetOrCreateUserQuotaUseCase(quotas, prices, users); }

    @Bean
    public MarkQuotaPaidUseCase markQuotaPaidUseCase(QuotaRepository repository) { return new MarkQuotaPaidUseCase(repository); }

    @Bean
    public StripeCheckoutGateway stripeCheckoutGateway(StripeProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) { return new StripeCheckoutGatewayAdapter(properties, objectMapper, meterRegistry); }

    @Bean
    public CreateStripeQuotaCheckoutUseCase createStripeQuotaCheckoutUseCase(GetOrCreateUserQuotaUseCase quotas, QuotaRepository quotaRepository, StripeCheckoutGateway gateway) { return new CreateStripeQuotaCheckoutUseCase(quotas, quotaRepository, gateway); }

    @Bean
    public ConfirmStripeQuotaPaymentUseCase confirmStripeQuotaPaymentUseCase(QuotaRepository repository, UserRepository users, EmailService emails) { return new ConfirmStripeQuotaPaymentUseCase(repository, users, emails); }

    @Bean
    public CreateStripeRegistrationCheckoutUseCase createStripeRegistrationCheckoutUseCase(RegistrationRepository registrations, RegistrationCheckoutRepository checkouts, CategoryRepository categories, StripeCheckoutGateway stripe) {
        return new CreateStripeRegistrationCheckoutUseCase(registrations, checkouts, categories, stripe);
    }

    @Bean
    public ConfirmStripeRegistrationPaymentUseCase confirmStripeRegistrationPaymentUseCase(RegistrationCheckoutRepository checkouts, RegistrationRepository registrations, UserRepository users, ChampionshipRepository championships, EmailService emails) {
        return new ConfirmStripeRegistrationPaymentUseCase(checkouts, registrations, users, championships, emails);
    }

    @Bean
    public CreateStripeChampionshipBasketCheckoutUseCase createStripeChampionshipBasketCheckoutUseCase(UserRepository users, ChampionshipRepository championships, CategoryRepository categories, QuotaRepository quotas, ChampionshipAgeCategoryPriceRepository prices, ChampionshipRegistrationPriceCalculator calculator, RegistrationCheckoutRepository checkouts, StripeCheckoutGateway stripe, RegistrationRepository registrations, RegisterForChampionshipUseCase registerForChampionship) {
        return new CreateStripeChampionshipBasketCheckoutUseCase(users, championships, categories, quotas, prices, calculator, checkouts, stripe, registrations, registerForChampionship);
    }

    @Bean
    public ConfirmStripeChampionshipBasketPaymentUseCase confirmStripeChampionshipBasketPaymentUseCase(RegistrationCheckoutRepository checkouts, RegistrationRepository repository, UserRepository users, ChampionshipRepository championships, EmailService emails) {
        return new ConfirmStripeChampionshipBasketPaymentUseCase(checkouts, repository, users, championships, emails);
    }

    @Bean
    public StripeWebhookVerifier stripeWebhookVerifier(StripeProperties properties) { return new StripeWebhookVerifier(properties); }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CreateUserUseCase createUserUseCase(
            UserRepository userRepository,
            BCryptPasswordEncoder encoder, EmailService emailService
    ) {
        return new CreateUserUseCase(userRepository, encoder::encode, emailService);
    }

    @Bean
    public LoginUseCase loginUseCase(
            UserRepository userRepository,
            BCryptPasswordEncoder encoder
    ) {
        return new LoginUseCase(userRepository, encoder::matches);
    }

    @Bean
    public IssueRefreshTokenUseCase issueRefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${aelb.jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        return new IssueRefreshTokenUseCase(refreshTokenRepository, Duration.ofMillis(refreshExpirationMs));
    }

    @Bean
    public RefreshSessionUseCase refreshSessionUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            IssueRefreshTokenUseCase issueRefreshTokenUseCase
    ) {
        return new RefreshSessionUseCase(refreshTokenRepository, userRepository, issueRefreshTokenUseCase);
    }

    @Bean
    public LogoutUseCase logoutUseCase(RefreshTokenRepository refreshTokenRepository) {
        return new LogoutUseCase(refreshTokenRepository);
    }

    @Bean
    public ChangePasswordUseCase changePasswordUseCase(
            UserRepository userRepository,
            BCryptPasswordEncoder encoder
    ) {
        return new ChangePasswordUseCase(userRepository, encoder::matches, encoder::encode);
    }

    @Bean public RequestPasswordResetUseCase requestPasswordResetUseCase(EmailService emails) { return new RequestPasswordResetUseCase(emails); }
    @Bean public ResetPasswordUseCase resetPasswordUseCase(SpringPasswordResetTokenJpaRepository tokens, UserRepository users, BCryptPasswordEncoder encoder) { return new ResetPasswordUseCase(tokens, users, encoder::encode); }
    @Bean public VerifyEmailUseCase verifyEmailUseCase(SpringEmailVerificationTokenJpaRepository tokens, UserRepository users) { return new VerifyEmailUseCase(tokens, users); }
    @Bean public ResendEmailVerificationUseCase resendEmailVerificationUseCase(UserRepository users, EmailService emails) { return new ResendEmailVerificationUseCase(users, emails); }

    @Bean
    public UpdateProfileUseCase updateProfileUseCase(UserRepository userRepository) {
        return new UpdateProfileUseCase(userRepository);
    }

    @Bean
    public ManagementUpdateProfileUseCase managementUpdateProfileUseCase(UserRepository userRepository) {
        return new ManagementUpdateProfileUseCase(userRepository);
    }

    @Bean
    public AdminUpdateUserUseCase adminUpdateUserUseCase(UserRepository userRepository) {
        return new AdminUpdateUserUseCase(userRepository);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepository userRepository) {
        return new DeleteUserUseCase(userRepository);
    }

    @Bean
    public CreateCategoryUseCase createCategoryUseCase(CategoryRepository categoryRepository) {
        return new CreateCategoryUseCase(categoryRepository);
    }

    @Bean
    public UpdateCategoryUseCase updateCategoryUseCase(CategoryRepository categoryRepository) {
        return new UpdateCategoryUseCase(categoryRepository);
    }

    @Bean
    public DeleteCategoryUseCase deleteCategoryUseCase(CategoryRepository categoryRepository) {
        return new DeleteCategoryUseCase(categoryRepository);
    }

    @Bean
    public CreateChampionshipUseCase createChampionshipUseCase(ChampionshipRepository championshipRepository) {
        return new CreateChampionshipUseCase(championshipRepository);
    }

    @Bean
    public UpdateChampionshipUseCase updateChampionshipUseCase(ChampionshipRepository championshipRepository) {
        return new UpdateChampionshipUseCase(championshipRepository);
    }

    @Bean
    public DeleteChampionshipUseCase deleteChampionshipUseCase(ChampionshipRepository championshipRepository) {
        return new DeleteChampionshipUseCase(championshipRepository);
    }

    @Bean
    public CreateNewsUseCase createNewsUseCase(NewsRepository newsRepository) {
        return new CreateNewsUseCase(newsRepository);
    }

    @Bean
    public UpdateNewsUseCase updateNewsUseCase(NewsRepository newsRepository) {
        return new UpdateNewsUseCase(newsRepository);
    }

    @Bean
    public DeleteNewsUseCase deleteNewsUseCase(NewsRepository newsRepository) {
        return new DeleteNewsUseCase(newsRepository);
    }

    @Bean
    public CreateResultUseCase createResultUseCase(ResultRepository resultRepository, FileStorage fileStorage) {
        return new CreateResultUseCase(resultRepository, fileStorage);
    }

    @Bean
    public UpdateResultUseCase updateResultUseCase(ResultRepository resultRepository, FileStorage fileStorage) {
        return new UpdateResultUseCase(resultRepository, fileStorage);
    }

    @Bean
    public DeleteResultUseCase deleteResultUseCase(ResultRepository resultRepository, FileStorage fileStorage) {
        return new DeleteResultUseCase(resultRepository, fileStorage);
    }

    @Bean
    public CancelRegistrationUseCase cancelRegistrationUseCase(RegistrationRepository registrationRepository) {
        return new CancelRegistrationUseCase(registrationRepository);
    }

    @Bean
    public UpdatePaymentStatusUseCase updatePaymentStatusUseCase(RegistrationRepository registrationRepository, UserRepository users, ChampionshipRepository championships, EmailService emails) {
        return new UpdatePaymentStatusUseCase(registrationRepository, users, championships, emails);
    }

    @Bean
    public ChangeRegistrationCategoryUseCase changeRegistrationCategoryUseCase(
            RegistrationRepository registrationRepository,
            ChampionshipRepository championshipRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        return new ChangeRegistrationCategoryUseCase(registrationRepository, championshipRepository, categoryRepository, userRepository);
    }

    @Bean
    public PricingEngine pricingEngine() {
        return new PricingEngine();
    }

    @Bean
    public ChampionshipRegistrationPriceCalculator championshipRegistrationPriceCalculator() {
        return new ChampionshipRegistrationPriceCalculator();
    }

    @Bean
    public SavePricingConfigUseCase savePricingConfigUseCase(PricingConfigRepository pricingConfigRepository) {
        return new SavePricingConfigUseCase(pricingConfigRepository);
    }

    @Bean
    public GetPricingConfigUseCase getPricingConfigUseCase(PricingConfigRepository pricingConfigRepository) {
        return new GetPricingConfigUseCase(pricingConfigRepository);
    }

    @Bean
    public CalculatePriceUseCase calculatePriceUseCase(
            PricingConfigRepository pricingConfigRepository,
            PricingEngine pricingEngine
    ) {
        return new CalculatePriceUseCase(pricingConfigRepository, pricingEngine);
    }

    @Bean
    public PrepareCheckoutUseCase prepareCheckoutUseCase(
            PricingConfigRepository pricingConfigRepository,
            RegistrationCheckoutRepository checkoutRepository,
            PricingEngine pricingEngine
    ) {
        return new PrepareCheckoutUseCase(pricingConfigRepository, checkoutRepository, pricingEngine);
    }

    @Bean
    public ApplyPricingTemplateUseCase applyPricingTemplateUseCase(
            SavePricingConfigUseCase savePricingConfigUseCase
    ) {
        return new ApplyPricingTemplateUseCase(savePricingConfigUseCase);
    }

    @Bean
    public CreateDocumentUseCase createDocumentUseCase(DocumentRepository documentRepository, FileStorage fileStorage) {
        return new CreateDocumentUseCase(documentRepository, fileStorage);
    }

    @Bean
    public UpdateDocumentUseCase updateDocumentUseCase(DocumentRepository documentRepository, FileStorage fileStorage) {
        return new UpdateDocumentUseCase(documentRepository, fileStorage);
    }

    @Bean
    public DeleteDocumentUseCase deleteDocumentUseCase(DocumentRepository documentRepository, FileStorage fileStorage) {
        return new DeleteDocumentUseCase(documentRepository, fileStorage);
    }

    @Bean
    public CreateTeamUseCase createTeamUseCase(TeamRepository teamRepository) {
        return new CreateTeamUseCase(teamRepository);
    }

    @Bean
    public UpdateTeamUseCase updateTeamUseCase(TeamRepository teamRepository) {
        return new UpdateTeamUseCase(teamRepository);
    }

    @Bean
    public DeleteTeamUseCase deleteTeamUseCase(TeamRepository teamRepository) {
        return new DeleteTeamUseCase(teamRepository);
    }

    @Bean
    public RegisterForChampionshipUseCase registerForChampionshipUseCase(
            UserRepository userRepository,
            ChampionshipRepository championshipRepository,
            RegistrationRepository registrationRepository,
            CategoryRepository categoryRepository,
            QuotaRepository quotaRepository,
            ChampionshipAgeCategoryPriceRepository categoryPriceRepository,
            ChampionshipRegistrationPriceCalculator priceCalculator,
            EmailService emailService
    ) {
        return new RegisterForChampionshipUseCase(
                userRepository, championshipRepository, registrationRepository, categoryRepository, quotaRepository, categoryPriceRepository, priceCalculator, emailService
        );
    }
}
