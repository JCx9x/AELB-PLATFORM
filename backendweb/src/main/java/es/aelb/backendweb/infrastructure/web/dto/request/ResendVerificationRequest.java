package es.aelb.backendweb.infrastructure.web.dto.request;

import jakarta.validation.constraints.Email;

/** Email opcional: solo lo usa un caller anónimo; uno autenticado reenvía para su propia sesión. */
public record ResendVerificationRequest(@Email String email) {}
