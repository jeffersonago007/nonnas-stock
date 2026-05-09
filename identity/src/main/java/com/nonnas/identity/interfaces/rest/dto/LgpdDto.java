package com.nonnas.identity.interfaces.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public final class LgpdDto {

    /**
     * Pelo menos um dos campos deve ser informado. Validação fina fica no
     * service (ele só atualiza o que veio não-vazio).
     */
    public record CorrigirRequest(
            @Size(max = 255) String nome,
            @Email @Size(max = 255) String email
    ) {}

    private LgpdDto() {}
}
