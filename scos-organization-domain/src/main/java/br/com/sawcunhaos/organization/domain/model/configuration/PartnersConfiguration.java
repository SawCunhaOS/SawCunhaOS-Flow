package br.com.sawcunhaos.organization.domain.model.configuration;

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "IFP_CONFIGURATION")
@Auditable
public class PartnersConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONFIGURATION_ID")
    private Long id;
    @Column(name = "CONFIGURATION_KEY")
    @Enumerated(EnumType.STRING)
    private ConfigurationKey key;
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "CONFIGURATION_VALUE")
    private String value;
    @Column(name = "DEFAULT_VALUE")
    private String defaultValue;

    public String getValueOrDefaulValue() {
        return Objects.nonNull(value) ? value : defaultValue;
    }
}
