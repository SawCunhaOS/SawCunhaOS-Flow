package br.com.sawcunhaos.organization.api.enumeration;

import br.com.sawcunhaos.foundation.utils.sort.PropertiesOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CompanyOrder implements PropertiesOrder {
    ID("id"),
    NAME("name"),
    NAME_TREATMENT("nameTreatment"),
    TAX_IDENTIFIER("taxIdentifier"),
    FOUNDATION_DATE("foundationDate")
    ;

    private final String properties;

    @Override
    public String properties() {
        return this.getProperties();
    }

    @Override
    public String value(String name) {
        try {
            return CompanyOrder.valueOf(name).getProperties();
        } catch (Exception exception) {
            return this.getProperties();
        }
    }
}
