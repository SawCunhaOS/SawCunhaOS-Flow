package br.com.sawcunhaos.organization.infrastructure.enumaration;

import br.com.sawcunhaos.security.starter.specification.ScosPermission;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScosOrganizationPermission implements ScosPermission {

    CREATE_DEPARTMENT(
            "Criar departamento",
            "Create department",
            true
    ),
    GET_DEPARTMENT(
            "Consultar departamento",
            "Get department",
            true
    ),
    UPDATE_DEPARTMENT(
            "Atualizar departamento",
            "Update department",
            true

    ),
    DELETE_DEPARTMENT(
            "Excluir departamento",
            "Delete department",
            true
    ),

    GET_POSITION(
            "Consultar cargo",
            "Get position",
            true
    ),
    CREATE_POSITION(
            "Criar cargo",
            "Create position",
            true
    ),
    UPDATE_POSITION(
            "Atualizar cargo",
            "Update position",
            true
    ),
    DELETE_POSITION(
            "Excluir cargo",
            "Delete position",
            true
    ),
    GET_COMPANY(
            "Consultar empresa",
            "Get company",
            true
    ),
    CREATE_COMPANY(
            "Criar empresa",
            "Create company",
            true
    ),
    UPDATE_COMPANY(
            "Atualizar empresa",
            "Update company",
            true
    ),
    ENABLE_COMPANY(
            "Ativar empresa",
            "Enable company",
            true
    ),
    DISABLE_COMPANY(
            "Inativar empresa",
            "Disable company",
            true
    ),
    DELETE_COMPANY(
            "Excluir empresa",
            "Delete company",
            true
    ),
    GET_COMPANY_ADDRESS(
            "Consultar endereço da empresa",
            "Get company address",
            true
    ),
    CREATE_COMPANY_ADDRESS(
            "Adicionar endereço na empresa",
            "Add company address",
            true
    ),
    UPDATE_COMPANY_ADDRESS(
            "Atualizar endereço da empresa",
            "Update company address",
            true
    ),
    DELETE_COMPANY_ADDRESS(
            "Excluir endereço da empresa",
            "Delete company address",
            true
    ),
    GET_COMPANY_CONTACT(
            "Consultar contatos da empresa",
            "Get company contact",
            true
    ),
    CREATE_COMPANY_CONTACT(
            "Adicionar contatos na empresa",
            "Add company contact",
            true
    ),
    UPDATE_COMPANY_CONTACT(
            "Atualizar contatos da empresa",
            "Update company contact",
            true
    ),
    DELETE_COMPANY_CONTACT(
            "Excluir contatos da empresa",
            "Delete company contact",
            true
    ),
    GET_EMPLOYEE(
            "Consultar funcionário",
            "Get employee",
            true
    ),
    CREATE_EMPLOYEE(
            "Criar funcionário",
            "Create employee",
            true
    ),
    UPDATE_EMPLOYEE(
            "Atualizar funcionário",
            "Update employee",
            true
    ),
    ENABLE_EMPLOYEE(
            "Ativar funcionário",
            "Enable employee",
            true
    ),

    ;

    private final String descriptionPtBr;
    private final String descriptionEng;
    private final Boolean active;


    @Override
    public String getPermission() {
        return this.name();
    }
}
