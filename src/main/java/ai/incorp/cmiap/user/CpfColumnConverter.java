package ai.incorp.cmiap.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter
public class CpfColumnConverter implements AttributeConverter<String, Long> {

    @Override
    public Long convertToDatabaseColumn(String cpf) {
        return cpf == null ? null : Long.valueOf(cpf);
    }

    @Override
    public String convertToEntityAttribute(Long cpf) {
        return cpf == null ? null : String.format(Locale.ROOT, "%011d", cpf);
    }
}
