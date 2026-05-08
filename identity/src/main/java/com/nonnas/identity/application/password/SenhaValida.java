package com.nonnas.identity.application.password;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean-Validation constraint that enforces the password policy described in
 * master doc section 13.3: ≥10 chars, ≥1 letter, ≥1 number, ≥1 special.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhaValidaValidator.class)
public @interface SenhaValida {
    String message() default "Senha não atende à política: mínimo 10 caracteres, ao menos 1 letra, 1 número e 1 caractere especial";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
