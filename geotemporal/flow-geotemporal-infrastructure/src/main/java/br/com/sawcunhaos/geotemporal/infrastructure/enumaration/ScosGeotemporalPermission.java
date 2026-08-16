package br.com.sawcunhaos.geotemporal.infrastructure.enumaration;

import br.com.sawcunhaos.security.starter.specification.ScosPermission;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * Permissões do módulo geotemporal, referenciadas pelo {@code x-authorize} do contrato OpenAPI.
 * Cada constante carrega os metadados da definição (grupo, subgrupo, versão, data de atualização,
 * chave i18n da descrição). As descrições pt-br/en vivem nos bundles {@code messages_permission*}.
 *
 * <p>Convenção: qualquer alteração da definição de uma permissão MUST bumpar {@code updatedAt}
 * (o upsert condicional do resource só atualiza quando {@code active} ou {@code updatedAt} muda).
 */
@Getter
@RequiredArgsConstructor
public enum ScosGeotemporalPermission implements ScosPermission {

    ;

    private final String codeDescription;
    private final String group;
    private final String subGroup;
    private final String version;
    @Getter(AccessLevel.NONE)
    private final String updatedAt;
    private final Boolean active;

    @Override
    public String getPermission() {
        return this.name();
    }

    @Override
    public LocalDate getUpdatedAt() {
        return LocalDate.parse(updatedAt);
    }
}
