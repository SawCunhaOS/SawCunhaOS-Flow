package br.com.sawcunhaos.organization.infrastructure.enumaration;

import br.com.sawcunhaos.foundation.utils.specification.ScosFeature;
import br.com.sawcunhaos.foundation.utils.specification.ScosPermission;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ScosOrganizationPermission implements ScosPermission {

    CREATE_DEPARTMENT(
            "Criar departamento",
            "Create department",
            "/v1/departments",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_DEPARTMENT_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    GET_DEPARTMENT(
            "Consultar departamento",
            "Get department",
            "/v1/departments/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_DEPARTMENT_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
            )
    ),
    UPDATE_DEPARTMENT(
            "Atualizar departamento",
            "Update department",
            "/v1/departments/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_DEPARTMENT_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )

    ),
    DELETE_DEPARTMENT(
            "Excluir departamento",
            "Delete department",
            "/v1/departments/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_DEPARTMENT_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),

    GET_POSITION(
            "Consultar cargo",
            "Get position",
            "/v1/positions/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_POSITION_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
            )
    ),
    CREATE_POSITION(
            "Criar cargo",
            "Create position",
            "/v1/positions",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_POSITION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_POSITION(
            "Atualizar cargo",
            "Update position",
            "/v1/positions/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_POSITION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    DELETE_POSITION(
            "Excluir cargo",
            "Delete position",
            "/v1/positions/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_POSITION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    GET_COMPANY(
            "Consultar empresa",
            "Get company",
            "/v1/company/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
            )
    ),
    CREATE_COMPANY(
            "Criar empresa",
            "Create company",
            "/v1/company",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_COMPANY(
            "Atualizar empresa",
            "Update company",
            "/v1/company/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    ENABLE_COMPANY(
            "Ativar empresa",
            "Enable company",
            "/v1/company/{id}/enable",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT
            )
    ),
    DISABLE_COMPANY(
            "Inativar empresa",
            "Disable company",
            "/v1/company/{id}/disable",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT
            )
    ),
    DELETE_COMPANY(
            "Excluir empresa",
            "Delete company",
            "/v1/company/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    GET_COMPANY_ADDRESS(
            "Consultar endereço da empresa",
            "Get company address",
            "/v1/company/{id}/address/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    CREATE_COMPANY_ADDRESS(
            "Adicionar endereço na empresa",
            "Add company address",
            "/v1/company/{id}/address",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_COMPANY_ADDRESS(
            "Atualizar endereço da empresa",
            "Update company address",
            "/v1/company/{id}/address/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    DELETE_COMPANY_ADDRESS(
            "Excluir endereço da empresa",
            "Delete company address",
            "/v1/company/{id}/address/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    GET_COMPANY_CONTACT(
            "Consultar contatos da empresa",
            "Get company contact",
            "/v1/company/{id}/contact/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT
            )
    ),
    CREATE_COMPANY_CONTACT(
            "Adicionar contatos na empresa",
            "Add company contact",
            "/v1/company/{id}/contact",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_COMPANY_CONTACT(
            "Atualizar contatos da empresa",
            "Update company contact",
            "/v1/company/{id}/contact/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    DELETE_COMPANY_CONTACT(
            "Excluir contatos da empresa",
            "Delete company contact",
            "/v1/company/{id}/contact/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    GET_EMPLOYEE(
            "Consultar funcionário",
            "Get employee",
            "/v1/employee/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
            )
    ),
    CREATE_EMPLOYEE(
            "Criar funcionário",
            "Create employee",
            "/v1/employee",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_EMPLOYEE(
            "Atualizar funcionário",
            "Update employee",
            "/v1/employee/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
            )
    ),
    ENABLE_EMPLOYEE(
            "Ativar funcionário",
            "Enable employee",
            "/v1/employee/{id}/enable",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
            )
    ),
    DISABLE_EMPLOYEE(
            "Inativar funcionário",
            "Disable employee",
            "/v1/employee/{id}/disable",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT
            )
    ),
    DELETE_EMPLOYEE(
            "Excluir funcionário",
            "Delete employee",
            "/v1/employee/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    GET_PROFILE(
            "Consultar perfil",
            "Get profile",
            "/v1/profile/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_PROFILE_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT
            )
    ),
    CREATE_PROFILE(
            "Criar perfil",
            "Create profile",
            "/v1/profile",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_PROFILE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_ADMINISTRATION
            )
    ),
    UPDATE_PROFILE(
            "Atualizar perfil",
            "Update profile",
            "/v1/profile/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_PROFILE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_ADMINISTRATION
            )
    ),
    DELETE_PROFILE(
            "Excluir perfil",
            "Delete profile",
            "/v1/profile/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_PROFILE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_ADMINISTRATION
            )
    ),

    /* Employee contact/address permissions */
    GET_EMPLOYEE_CONTACT(
            "Consultar contatos do empregado",
            "Get employee contact",
            "/v1/employee/{employeeId}/contact",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    CREATE_EMPLOYEE_CONTACT(
            "Criar contato do empregado",
            "Create employee contact",
            "/v1/employee/{employeeId}/contact",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_EMPLOYEE_CONTACT(
            "Atualizar contato do empregado",
            "Update employee contact",
            "/v1/employee/{employeeId}/contact/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    DELETE_EMPLOYEE_CONTACT(
            "Excluir contato do empregado",
            "Delete employee contact",
            "/v1/employee/{employeeId}/contact/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),

    GET_EMPLOYEE_ADDRESS(
            "Consultar endereço do empregado",
            "Get employee address",
            "/v1/employee/{employeeId}/address",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    CREATE_EMPLOYEE_ADDRESS(
            "Criar endereço do empregado",
            "Create employee address",
            "/v1/employee/{employeeId}/address",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_EMPLOYEE_ADDRESS(
            "Atualizar endereço do empregado",
            "Update employee address",
            "/v1/employee/{employeeId}/address/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    DELETE_EMPLOYEE_ADDRESS(
            "Excluir endereço do empregado",
            "Delete employee address",
            "/v1/employee/{employeeId}/address/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),

    /* Employee login permissions */
    GET_EMPLOYEE_LOGIN(
            "Consultar login do empregado",
            "Get employee login",
            "/v1/employee/{employeeId}/login",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_EMPLOYEE_LOGIN(
            "Atualizar login do empregado",
            "Update employee login",
            "/v1/employee/{employeeId}/login/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    DELETE_EMPLOYEE_LOGIN(
            "Excluir login do empregado",
            "Delete employee login",
            "/v1/employee/{employeeId}/login/{id}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_EMPLOYEE_LOGIN_STATUS(
            "Atualizar status do login",
            "Update employee login status",
            "/v1/employee/{employeeId}/login/{id}/block",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    UPDATE_EMPLOYEE_LOGIN_PASSWORD(
            "Atualizar senha do login",
            "Update employee login password",
            "/v1/employee/{employeeId}/login/{id}/password",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    GET_EMPLOYEE_LOGIN_INFO(
            "Consultar informações do login",
            "Get employee login info",
            "/v1/employee/login/info",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_LOGIN_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_EMPLOYEE_MANAGEMENT
            )
    ),

    /* Features and documents (commented endpoints also covered) */
    GET_FEATURES(
            "Consultar funcionalidades",
            "Get features",
            "/v1/features",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_ADMINISTRATION,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),

    GET_COMPANY_DOCUMENT(
            "Consultar documentos da empresa",
            "Get company document",
            "/v1/company/{companyId}/document",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT
            )
    ),
    CREATE_COMPANY_DOCUMENT(
            "Criar documento da empresa",
            "Create company document",
            "/v1/company/{companyId}/document",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    DELETE_COMPANY_DOCUMENT(
            "Excluir documento da empresa",
            "Delete company document",
            "/v1/company/{companyId}/document/{idRequest}",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT,
                    ScosOrganizationFeature.ORGANIZATION_MANAGEMENT
            )
    ),
    DOWNLOAD_COMPANY_DOCUMENT(
            "Download documento da empresa",
            "Download company document",
            "/v1/company/{companyId}/document/{idRequest}/download",
            "ORGANIZATION",
            List.of(
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_VIEW,
                    ScosOrganizationFeature.ORGANIZATION_COMPANY_MANAGEMENT
            )
    ),

    ;

    private final String descriptionPtBr;
    private final String descriptionEng;
    private final String endPoint;
    private final String module;
    private final List<ScosFeature> features;


    @Override
    public String getPermission() {
        return this.name();
    }
}
