package br.com.sawcunhaos.organization.infrastructure.enumaration;

import br.com.sawcunhaos.foundation.utils.specification.ScosFeature;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ScosOrganizationFeature implements ScosFeature {

    ORGANIZATION_ADMINISTRATION(
            "ORGANIZATION_ADMINISTRATION",
            "Administração",
            "Administration",
            "ORGANIZATION"
    ),
    ORGANIZATION_VIEW(
            "ORGANIZATION_VIEW",
            "Visualização geral",
            "General view",
            "ORGANIZATION"
    ),
    ORGANIZATION_MANAGEMENT(
            "ORGANIZATION_MANAGEMENT",
            "Gerenciamento geral",
            "General management",
            "ORGANIZATION"
    ),

    ORGANIZATION_COMPANY_MANAGEMENT(
            "ORGANIZATION_COMPANY_MANAGEMENT",
            "Gerenciamento geral de empresas",
            "General company management",
            "ORGANIZATION"
    ),
    ORGANIZATION_COMPANY_VIEW(
            "ORGANIZATION_COMPANY_VIEW",
            "Visualização geral de empresas",
            "General company view",
            "ORGANIZATION"
    ),

    ORGANIZATION_EMPLOYEE_VIEW(
            "ORGANIZATION_EMPLOYEE_VIEW",
            "Visualização de colaboradores",
            "Employee view",
            "ORGANIZATION"
    ),
    ORGANIZATION_EMPLOYEE_MANAGEMENT(
            "ORGANIZATION_EMPLOYEE_MANAGEMENT",
            "Gerenciamento geral de colaboradores",
            "General employee management",
            "ORGANIZATION"
    ),
    ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT(
            "ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT",
            "Gerenciamento de login de colaboradores",
            "Employee login management",
            "ORGANIZATION"
    ),

    ORGANIZATION_PROFILE_VIEW(
            "ORGANIZATION_PROFILE_VIEW",
            "Visualização de perfis",
            "Profile view",
            "ORGANIZATION"
    ),
    ORGANIZATION_PROFILE_MANAGEMENT(
            "ORGANIZATION_PROFILE_MANAGEMENT",
            "Gerenciamento de perfis",
            "Profile management",
            "ORGANIZATION"
    ),

    ORGANIZATION_DEPARTMENT_VIEW(
            "ORGANIZATION_DEPARTMENT_VIEW",
            "Visualização de departamentos",
            "Department view",
            "ORGANIZATION"
    ),
    ORGANIZATION_DEPARTMENT_MANAGEMENT(
            "ORGANIZATION_DEPARTMENT_MANAGEMENT",
            "Gerenciamento de departamentos",
            "Department management",
            "ORGANIZATION"
    ),

    ORGANIZATION_POSITION_VIEW(
            "ORGANIZATION_POSITION_VIEW",
            "Visualização de cargos",
            "Position view",
            "ORGANIZATION"
    ),
    ORGANIZATION_POSITION_MANAGEMENT(
            "ORGANIZATION_POSITION_MANAGEMENT",
            "Gerenciamento de cargos",
            "Position management",
            "ORGANIZATION"
    )


    ;

    private final String feature;
    private final String descriptionPtBr;
    private final String descriptionEng;
    private final String module;

}
