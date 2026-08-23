package br.com.sawcunhaos.organization.infrastructure.enumaration;

import br.com.sawcunhaos.security.starter.specification.ScosPermission;
import br.com.sawcunhaos.security.starter.specification.ScosSystem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ScosOrganizationSystem implements ScosSystem {

    ORGANIZATION_SYSTEM(
            "Scos Organization",
            "SCOS_ORGANIZATION",
            "Sistema de Gestão de Empresas e Funcionarios",
            Arrays.stream(ScosOrganizationPermission.values())
                    .map(p -> (ScosPermission) p).toList()
    );

    private final String name;
    private final String code;
    private final String description;
    private final List<ScosPermission> permissions;



    @Override
    public String getSystemName() {
        return name;
    }

    @Override
    public String getSystemCode() {
        return code;
    }

    @Override
    public String getSystemDescription() {
        return description;
    }

    @Override
    public List<ScosPermission> getPermissions() {
        return permissions;
    }
}
