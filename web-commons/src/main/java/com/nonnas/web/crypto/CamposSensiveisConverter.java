package com.nonnas.web.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter que criptografa o atributo de domínio em
 * AES-GCM antes de persistir e descriptografa ao ler. Aplicar via
 * {@code @Convert(converter = CamposSensiveisConverter.class)} em
 * colunas com dado pessoal protegido pela LGPD (CPF, telefone pessoal,
 * endereço residencial, nome/contato de cliente final).
 *
 * <p>Não aplicar em colunas usadas em WHERE/UNIQUE (login, índice
 * único) — o ciphertext é não-determinístico (IV aleatório) e quebra
 * busca por igualdade. Pra esses casos usar HMAC indexável + cifra
 * (fora do escopo).
 *
 * <p>Spring Boot 3 + Hibernate 6 suportam autowiring em AttributeConverters
 * via {@code @Component}.
 */
@Component
@Converter(autoApply = false)
public class CamposSensiveisConverter implements AttributeConverter<String, String> {

    private final CryptoService crypto;

    public CamposSensiveisConverter(CryptoService crypto) {
        this.crypto = crypto;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return crypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return crypto.decrypt(dbData);
    }
}
