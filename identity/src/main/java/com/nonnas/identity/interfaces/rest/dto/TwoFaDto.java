package com.nonnas.identity.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public final class TwoFaDto {

    public record SetupResponse(
            String secretBase32,
            String otpauthUri
    ) {}

    public record ConfirmarRequest(
            @NotBlank @Pattern(regexp = "\\d{6}", message = "Código TOTP precisa ter 6 dígitos")
            String codigo
    ) {}

    public record ConfirmarResponse(
            List<String> backupCodes
    ) {}

    private TwoFaDto() {}
}
