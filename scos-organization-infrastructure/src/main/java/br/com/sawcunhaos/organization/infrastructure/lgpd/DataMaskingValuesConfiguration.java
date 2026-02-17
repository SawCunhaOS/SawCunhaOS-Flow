package br.com.sawcunhaos.organization.infrastructure.lgpd;

import br.com.sawcunhaos.foundation.utils.lgpd.model.DataMask;
import br.com.sawcunhaos.foundation.utils.lgpd.specification.DataMaskingValues;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataMaskingValuesConfiguration implements DataMaskingValues {

    @Override
    public Set<DataMask> headersValue() {
        return Set.of(
                createDataMask("Authorization", "********", false, null)
        );
    }

    private DataMask createDataMask(
            final String key,
            final String newValue,
            final boolean isRegex,
            final String regex
    ) {
        return DataMask.builder()
                .key(key)
                .newValue(newValue)
                .isRegex(isRegex)
                .regex(regex)
                .build();
    }
}
