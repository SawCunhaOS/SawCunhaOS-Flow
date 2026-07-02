package br.com.sawcunhaos.organization.infrastructure.enumaration;

import br.com.sawcunhaos.security.starter.specification.ScosPermission;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScosOrganizationPermission implements ScosPermission {

    // Department
    GET_DEPARTMENT(
            "Consultar departamento",
            "Get department",
            true
    ),

    CREATE_DEPARTMENT(
            "Criar departamento",
            "Create department",
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

    ENABLE_DEPARTMENT(
            "Habilitar departamento",
            "Enable department",
            true
    ),

    DISABLE_DEPARTMENT(
            "Desabilitar departamento",
            "Disable department",
            true
    ),

    // Position
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

    ENABLE_POSITION(
            "Habilitar cargo",
            "Enable position",
            true
    ),

    DISABLE_POSITION(
            "Desabilitar cargo",
            "Disable position",
            true
    ),

    // Login
    GET_LOGIN(
            "Consultar login",
            "Get login",
            true
    ),

    GET_LOGIN_INFO(
            "Consultar informações de login",
            "Get login info",
            true
    ),

    CREATE_LOGIN(
            "Criar login",
            "Create login",
            true
    ),

    UPDATE_LOGIN(
            "Atualizar login",
            "Update login",
            true
    ),

    UPDATE_LOGIN_STATUS(
            "Atualizar status do login",
            "Update login status",
            true
    ),

    DELETE_LOGIN(
            "Excluir login",
            "Delete login",
            true
    ),

    // Profile
    GET_PROFILE(
            "Consultar perfil",
            "Get profile",
            true
    ),

    CREATE_PROFILE(
            "Criar perfil",
            "Create profile",
            true
    ),

    UPDATE_PROFILE(
            "Atualizar perfil",
            "Update profile",
            true
    ),

    DELETE_PROFILE(
            "Excluir perfil",
            "Delete profile",
            true
    ),

    ENABLE_PROFILE(
            "Habilitar perfil",
            "Enable profile",
            true
    ),

    DISABLE_PROFILE(
            "Desabilitar perfil",
            "Disable profile",
            true
    ),

    // Resource
    GET_RESOURCE(
            "Consultar recurso",
            "Get resource",
            true
    ),

    // Outbox
    GET_OUTBOX_EVENT(
            "Consultar evento do outbox",
            "Get outbox event",
            true
    ),

    RETRY_OUTBOX_EVENT(
            "Reprocessar evento do outbox",
            "Retry outbox event",
            true
    ),

    GET_OUTBOX_EVENT_DEAD_LETTER(
            "Consultar dead letter do outbox",
            "Get outbox event dead letter",
            true
    ),

    // Company
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

    DELETE_COMPANY(
            "Excluir empresa",
            "Delete company",
            true
    ),

    ENABLE_COMPANY(
            "Habilitar empresa",
            "Enable company",
            true
    ),

    DISABLE_COMPANY(
            "Desabilitar empresa",
            "Disable company",
            true
    ),

    // Company Contact
    GET_COMPANY_CONTACT(
            "Consultar contato da empresa",
            "Get company contact",
            true
    ),

    CREATE_COMPANY_CONTACT(
            "Criar contato da empresa",
            "Create company contact",
            true
    ),

    UPDATE_COMPANY_CONTACT(
            "Atualizar contato da empresa",
            "Update company contact",
            true
    ),

    DELETE_COMPANY_CONTACT(
            "Excluir contato da empresa",
            "Delete company contact",
            true
    ),

    // Company Address
    GET_COMPANY_ADDRESS(
            "Consultar endereço da empresa",
            "Get company address",
            true
    ),

    CREATE_COMPANY_ADDRESS(
            "Criar endereço da empresa",
            "Create company address",
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

    // Employee
    GET_EMPLOYEE(
            "Consultar colaborador",
            "Get employee",
            true
    ),

    CREATE_EMPLOYEE(
            "Criar colaborador",
            "Create employee",
            true
    ),

    UPDATE_EMPLOYEE(
            "Atualizar colaborador",
            "Update employee",
            true
    ),

    DELETE_EMPLOYEE(
            "Excluir colaborador",
            "Delete employee",
            true
    ),

    ENABLE_EMPLOYEE(
            "Habilitar colaborador",
            "Enable employee",
            true
    ),

    DISABLE_EMPLOYEE(
            "Desabilitar colaborador",
            "Disable employee",
            true
    ),

    TRANSFER_EMPLOYEE(
            "Transferir colaborador",
            "Transfer employee",
            true
    ),

    // Employee Contact
    GET_EMPLOYEE_CONTACT(
            "Consultar contato do colaborador",
            "Get employee contact",
            true
    ),

    CREATE_EMPLOYEE_CONTACT(
            "Criar contato do colaborador",
            "Create employee contact",
            true
    ),

    UPDATE_EMPLOYEE_CONTACT(
            "Atualizar contato do colaborador",
            "Update employee contact",
            true
    ),

    DELETE_EMPLOYEE_CONTACT(
            "Excluir contato do colaborador",
            "Delete employee contact",
            true
    ),

    // Employee Address
    GET_EMPLOYEE_ADDRESS(
            "Consultar endereço do colaborador",
            "Get employee address",
            true
    ),

    CREATE_EMPLOYEE_ADDRESS(
            "Criar endereço do colaborador",
            "Create employee address",
            true
    ),

    UPDATE_EMPLOYEE_ADDRESS(
            "Atualizar endereço do colaborador",
            "Update employee address",
            true
    ),

    DELETE_EMPLOYEE_ADDRESS(
            "Excluir endereço do colaborador",
            "Delete employee address",
            true
    ),

    // Configuration
    GET_CONFIGURATION(
            "Consultar configuração",
            "Get configuration",
            true
    ),

    UPDATE_CONFIGURATION(
            "Atualizar configuração",
            "Update configuration",
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
