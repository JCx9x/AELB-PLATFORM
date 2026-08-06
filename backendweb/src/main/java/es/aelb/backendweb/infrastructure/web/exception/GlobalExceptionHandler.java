package es.aelb.backendweb.infrastructure.web.exception;

import es.aelb.backendweb.application.auth.RefreshSessionUseCase;
import es.aelb.backendweb.application.registration.CancelRegistrationUseCase;
import es.aelb.backendweb.application.registration.ChangeRegistrationCategoryUseCase;
import es.aelb.backendweb.application.registration.RegisterForChampionshipUseCase;
import es.aelb.backendweb.application.user.ChangePasswordUseCase;
import es.aelb.backendweb.application.user.LoginUseCase;
import es.aelb.backendweb.application.user.UpdateProfileUseCase;
import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.news.News;
import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduce excepciones de dominio y validación a respuestas HTTP estandarizadas.
 * Usa RFC 9457 Problem Details (nativo en Spring Boot 3+).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoginUseCase.InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(LoginUseCase.InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(RefreshSessionUseCase.InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(RefreshSessionUseCase.InvalidRefreshTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(RefreshSessionUseCase.RefreshTokenReusedException.class)
    public ProblemDetail handleRefreshTokenReused(RefreshSessionUseCase.RefreshTokenReusedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(LoginUseCase.UserBlockedException.class)
    public ProblemDetail handleUserBlocked(LoginUseCase.UserBlockedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UpdateProfileUseCase.UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UpdateProfileUseCase.UserNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ChangePasswordUseCase.UserNotFoundException.class)
    public ProblemDetail handleChangePasswordUserNotFound(ChangePasswordUseCase.UserNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ChangePasswordUseCase.WrongCurrentPasswordException.class)
    public ProblemDetail handleWrongPassword(ChangePasswordUseCase.WrongCurrentPasswordException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(Category.NotFoundException.class)
    public ProblemDetail handleCategoryNotFound(Category.NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Championship.NotFoundException.class)
    public ProblemDetail handleChampionshipNotFound(Championship.NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(News.NotFoundException.class)
    public ProblemDetail handleNewsNotFound(News.NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Result.NotFoundException.class)
    public ProblemDetail handleResultNotFound(Result.NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RegisterForChampionshipUseCase.SameShiftConflictException.class)
    public ProblemDetail handleSameShiftConflict(RegisterForChampionshipUseCase.SameShiftConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CancelRegistrationUseCase.NotFoundException.class)
    public ProblemDetail handleRegistrationNotFound(CancelRegistrationUseCase.NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CancelRegistrationUseCase.ForbiddenOperationException.class)
    public ProblemDetail handleForbiddenRegistration(CancelRegistrationUseCase.ForbiddenOperationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CancelRegistrationUseCase.CancellationNotAllowedException.class)
    public ProblemDetail handleCancellationNotAllowed(CancelRegistrationUseCase.CancellationNotAllowedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ChangeRegistrationCategoryUseCase.NotFoundException.class)
    public ProblemDetail handleChangeCategoryRegistrationNotFound(ChangeRegistrationCategoryUseCase.NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }
}
