package br.com.sawcunhaos.organization.api.enumeration;

import br.com.sawcunhaos.foundation.utils.sort.PropertiesOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PositionOrder implements PropertiesOrder {
    ID("id"),
    CODE("code");

    private final String properties;

    @Override
    public String properties() {
        return this.getProperties();
    }

    @Override
    public String value(String name) {
        try {
            return PositionOrder.valueOf(name).getProperties();
        } catch (Exception exception) {
            return this.getProperties();
        }
    }
}
