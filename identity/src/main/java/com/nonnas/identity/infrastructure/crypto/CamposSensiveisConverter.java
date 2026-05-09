package com.nonnas.identity.infrastructure.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter que criptografa o atributo de domínio em
 * AES-GCM antes de persistir e descriptografa ao ler. Aplicar via
 * {@code @Convert(converter = CamposSensiveisConverter.class)} em
 * colunas com dado pessoal protegido pela LGPD (CPF, telefone pessoal,
 * endereço residencial). Para CNPJ-PF (CNPJ de microempreendedor
 * pessoal), aplicar quando o cadastro permitir esse modelo.
 *
 * <p>Spring Boot 3 + Hibernate 6 suportam autowiring em AttributeConverters
 * via {@code @Component}. Caso a integração falhe em algum ambiente, a
 * fallback explícita é injetar manualmente via Hibernate's BeanContainer.
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
