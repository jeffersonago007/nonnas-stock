package com.nonnas.web;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.DomainException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Tradutor centralizado de exceções para RFC 7807 Problem Details.
 * Vive em {@code web-commons} para que tanto o módulo {@code app} quanto
 * os ITs de cada bounded context possam reutilizar a mesma política
 * de erro sem duplicação.
 *
 * <p>Mapeamento:
 * <ul>
 *   <li>{@link ValidationException} → 400</li>
 *   <li>{@link NotFoundException} → 404</li>
 *   <li>{@link BusinessRuleException} → varia conforme {@link ErrorCode}</li>
 *   <li>{@link DomainException} (catch-all do domínio) → 422</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 com lista de campos</li>
 *   <li>{@link ConstraintViolationException} → 400</li>
 *   <li>{@link AccessDeniedException} → 403</li>
 *   <li>{@link RateLimitExceededException} → 429 com {@code Retry-After}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.code(), "Dados inválidos", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.code(), "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusiness(BusinessRuleException ex) {
        HttpStatus status = switch (ex.code()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return problem(status, ex.code(), "Regra de negócio violada", ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        log.warn("Unhandled domain exception", ex);
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), "Erro de domínio", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "Falha de validação", "Um ou mais campos são inválidos");
        pd.setProperty("errors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "Falha de validação", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Acesso negado",
                "Seu perfil não permite essa operação");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
        ProblemDetail pd = problem(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.CONFLICT,
                "Limite de requisições excedido", ex.getMessage());
        pd.setProperty("retryAfterSeconds", ex.retryAfterSeconds());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(pd);
    }

    private ProblemDetail problem(HttpStatus status, ErrorCode code, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("code", code.name());
        return pd;
    }
}
