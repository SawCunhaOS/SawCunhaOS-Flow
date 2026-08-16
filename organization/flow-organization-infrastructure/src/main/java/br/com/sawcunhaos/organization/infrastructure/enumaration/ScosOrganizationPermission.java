package br.com.sawcunhaos.organization.infrastructure.enumaration;

import br.com.sawcunhaos.security.starter.specification.ScosPermission;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * Permissões do módulo organization, referenciadas pelo {@code x-authorize} do contrato OpenAPI.
 * Cada constante carrega os metadados da definição (grupo, subgrupo, versão, data de atualização,
 * chave i18n da descrição). As descrições pt-br/en vivem nos bundles {@code messages_permission*}.
 *
 * <p>Convenção: qualquer alteração da definição de uma permissão MUST bumpar {@code updatedAt}
 * (o upsert condicional do resource só atualiza quando {@code active} ou {@code updatedAt} muda).
 */
@Getter
@RequiredArgsConstructor
public enum ScosOrganizationPermission implements ScosPermission {

    // Department
    GET_DEPARTMENT("GET_DEPARTMENT", "Corporate", "Department", "1.0.0", "2026-07-09", true),
    CREATE_DEPARTMENT("CREATE_DEPARTMENT", "Corporate", "Department", "1.0.0", "2026-07-09", true),
    UPDATE_DEPARTMENT("UPDATE_DEPARTMENT", "Corporate", "Department", "1.0.0", "2026-07-09", true),
    DELETE_DEPARTMENT("DELETE_DEPARTMENT", "Corporate", "Department", "1.0.0", "2026-07-09", true),
    ENABLE_DEPARTMENT("ENABLE_DEPARTMENT", "Corporate", "Department", "1.0.0", "2026-07-09", true),
    DISABLE_DEPARTMENT("DISABLE_DEPARTMENT", "Corporate", "Department", "1.0.0", "2026-07-09", true),

    // Position
    GET_POSITION("GET_POSITION", "Corporate", "Position", "1.0.0", "2026-07-09", true),
    CREATE_POSITION("CREATE_POSITION", "Corporate", "Position", "1.0.0", "2026-07-09", true),
    UPDATE_POSITION("UPDATE_POSITION", "Corporate", "Position", "1.0.0", "2026-07-09", true),
    DELETE_POSITION("DELETE_POSITION", "Corporate", "Position", "1.0.0", "2026-07-09", true),
    ENABLE_POSITION("ENABLE_POSITION", "Corporate", "Position", "1.0.0", "2026-07-09", true),
    DISABLE_POSITION("DISABLE_POSITION", "Corporate", "Position", "1.0.0", "2026-07-09", true),

    // Position Work Schedule
    GET_POSITION_WORK_SCHEDULE("GET_POSITION_WORK_SCHEDULE", "Corporate", "Position Work Schedule", "1.0.0", "2026-07-15", true),
    CREATE_POSITION_WORK_SCHEDULE("CREATE_POSITION_WORK_SCHEDULE", "Corporate", "Position Work Schedule", "1.0.0", "2026-07-15", true),
    UPDATE_POSITION_WORK_SCHEDULE("UPDATE_POSITION_WORK_SCHEDULE", "Corporate", "Position Work Schedule", "1.0.0", "2026-07-15", true),
    DELETE_POSITION_WORK_SCHEDULE("DELETE_POSITION_WORK_SCHEDULE", "Corporate", "Position Work Schedule", "1.0.0", "2026-07-15", true),

    // Login
    GET_LOGIN("GET_LOGIN", "Access", "Login", "1.0.0", "2026-07-09", true),
    GET_LOGIN_INFO("GET_LOGIN_INFO", "Access", "Login", "1.0.0", "2026-07-09", true),
    CREATE_LOGIN("CREATE_LOGIN", "Access", "Login", "1.0.0", "2026-07-09", true),
    UPDATE_LOGIN("UPDATE_LOGIN", "Access", "Login", "1.0.0", "2026-07-09", true),
    BLOCK_LOGIN("BLOCK_LOGIN", "Access", "Login", "1.0.0", "2026-08-16", true),
    UNBLOCK_LOGIN("UNBLOCK_LOGIN", "Access", "Login", "1.0.0", "2026-08-16", true),
    DELETE_LOGIN("DELETE_LOGIN", "Access", "Login", "1.0.0", "2026-07-09", true),
    ENABLE_LOGIN("ENABLE_LOGIN", "Access", "Login", "1.0.0", "2026-07-15", true),
    DISABLE_LOGIN("DISABLE_LOGIN", "Access", "Login", "1.0.0", "2026-07-15", true),
    GET_LOGIN_STATUS_HISTORY("GET_LOGIN_STATUS_HISTORY", "Access", "Login", "1.0.0", "2026-07-15", true),

    // Login Profile
    GET_LOGIN_PROFILE("GET_LOGIN_PROFILE", "Access", "Login Profile", "1.0.0", "2026-07-15", true),
    CREATE_LOGIN_PROFILE("CREATE_LOGIN_PROFILE", "Access", "Login Profile", "1.0.0", "2026-07-15", true),
    DELETE_LOGIN_PROFILE("DELETE_LOGIN_PROFILE", "Access", "Login Profile", "1.0.0", "2026-07-15", true),

    // Profile
    GET_PROFILE("GET_PROFILE", "Access", "Profile", "1.0.0", "2026-07-09", true),
    CREATE_PROFILE("CREATE_PROFILE", "Access", "Profile", "1.0.0", "2026-07-09", true),
    UPDATE_PROFILE("UPDATE_PROFILE", "Access", "Profile", "1.0.0", "2026-07-09", true),
    DELETE_PROFILE("DELETE_PROFILE", "Access", "Profile", "1.0.0", "2026-07-09", true),
    ENABLE_PROFILE("ENABLE_PROFILE", "Access", "Profile", "1.0.0", "2026-07-09", true),
    DISABLE_PROFILE("DISABLE_PROFILE", "Access", "Profile", "1.0.0", "2026-07-09", true),

    // Resource
    GET_RESOURCE("GET_RESOURCE", "Access", "Resource", "1.0.0", "2026-07-09", true),

    // System
    GET_SYSTEM("GET_SYSTEM", "Access", "System", "1.0.0", "2026-07-15", true),

    // Outbox
    GET_OUTBOX_EVENT("GET_OUTBOX_EVENT", "Outbox", "Outbox", "1.0.0", "2026-07-09", true),
    RETRY_OUTBOX_EVENT("RETRY_OUTBOX_EVENT", "Outbox", "Outbox", "1.0.0", "2026-07-09", true),
    GET_OUTBOX_EVENT_DEAD_LETTER("GET_OUTBOX_EVENT_DEAD_LETTER", "Outbox", "Outbox", "1.0.0", "2026-07-09", true),
    GET_OUTBOX_TOPIC("GET_OUTBOX_TOPIC", "Outbox", "Outbox", "1.0.0", "2026-07-15", true),

    // Company
    GET_COMPANY("GET_COMPANY", "Corporate", "Company", "1.0.0", "2026-07-09", true),
    CREATE_COMPANY("CREATE_COMPANY", "Corporate", "Company", "1.0.0", "2026-07-09", true),
    UPDATE_COMPANY("UPDATE_COMPANY", "Corporate", "Company", "1.0.0", "2026-07-09", true),
    DELETE_COMPANY("DELETE_COMPANY", "Corporate", "Company", "1.0.0", "2026-07-09", true),
    ENABLE_COMPANY("ENABLE_COMPANY", "Corporate", "Company", "1.0.0", "2026-07-09", true),
    DISABLE_COMPANY("DISABLE_COMPANY", "Corporate", "Company", "1.0.0", "2026-07-09", true),
    BLOCK_COMPANY("BLOCK_COMPANY", "Corporate", "Company", "1.0.0", "2026-07-15", true),
    UNBLOCK_COMPANY("UNBLOCK_COMPANY", "Corporate", "Company", "1.0.0", "2026-07-15", true),
    GET_COMPANY_STATUS_HISTORY("GET_COMPANY_STATUS_HISTORY", "Corporate", "Company", "1.0.0", "2026-07-15", true),

    // Company Cnae Secondary
    GET_COMPANY_CNAE_SECONDARY("GET_COMPANY_CNAE_SECONDARY", "Corporate", "Company Cnae Secondary", "1.0.0", "2026-07-15", true),
    CREATE_COMPANY_CNAE_SECONDARY("CREATE_COMPANY_CNAE_SECONDARY", "Corporate", "Company Cnae Secondary", "1.0.0", "2026-07-15", true),
    DELETE_COMPANY_CNAE_SECONDARY("DELETE_COMPANY_CNAE_SECONDARY", "Corporate", "Company Cnae Secondary", "1.0.0", "2026-07-15", true),

    // Company Contact
    GET_COMPANY_CONTACT("GET_COMPANY_CONTACT", "Corporate", "Company Contact", "1.0.0", "2026-07-09", true),
    CREATE_COMPANY_CONTACT("CREATE_COMPANY_CONTACT", "Corporate", "Company Contact", "1.0.0", "2026-07-09", true),
    UPDATE_COMPANY_CONTACT("UPDATE_COMPANY_CONTACT", "Corporate", "Company Contact", "1.0.0", "2026-07-09", true),
    DELETE_COMPANY_CONTACT("DELETE_COMPANY_CONTACT", "Corporate", "Company Contact", "1.0.0", "2026-07-09", true),

    // Company Address
    GET_COMPANY_ADDRESS("GET_COMPANY_ADDRESS", "Corporate", "Company Address", "1.0.0", "2026-07-09", true),
    CREATE_COMPANY_ADDRESS("CREATE_COMPANY_ADDRESS", "Corporate", "Company Address", "1.0.0", "2026-07-09", true),
    UPDATE_COMPANY_ADDRESS("UPDATE_COMPANY_ADDRESS", "Corporate", "Company Address", "1.0.0", "2026-07-09", true),
    DELETE_COMPANY_ADDRESS("DELETE_COMPANY_ADDRESS", "Corporate", "Company Address", "1.0.0", "2026-07-09", true),

    // Employee
    GET_EMPLOYEE("GET_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-09", true),
    CREATE_EMPLOYEE("CREATE_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-09", true),
    UPDATE_EMPLOYEE("UPDATE_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-09", true),
    DELETE_EMPLOYEE("DELETE_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-09", true),
    ENABLE_EMPLOYEE("ENABLE_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-09", true),
    DISABLE_EMPLOYEE("DISABLE_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-09", true),
    TRANSFER_EMPLOYEE("TRANSFER_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-09", true),
    REHIRE_EMPLOYEE("REHIRE_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-15", true),
    BLOCK_EMPLOYEE("BLOCK_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-15", true),
    UNBLOCK_EMPLOYEE("UNBLOCK_EMPLOYEE", "Corporate", "Employee", "1.0.0", "2026-07-15", true),
    GET_EMPLOYEE_STATUS_HISTORY("GET_EMPLOYEE_STATUS_HISTORY", "Corporate", "Employee", "1.0.0", "2026-07-15", true),
    GET_EMPLOYEE_POSITION_HISTORY("GET_EMPLOYEE_POSITION_HISTORY", "Corporate", "Employee", "1.0.0", "2026-07-15", true),

    // Employee Work Schedule
    GET_EMPLOYEE_WORK_SCHEDULE("GET_EMPLOYEE_WORK_SCHEDULE", "Corporate", "Employee Work Schedule", "1.0.0", "2026-07-15", true),
    CREATE_EMPLOYEE_WORK_SCHEDULE("CREATE_EMPLOYEE_WORK_SCHEDULE", "Corporate", "Employee Work Schedule", "1.0.0", "2026-07-15", true),
    UPDATE_EMPLOYEE_WORK_SCHEDULE("UPDATE_EMPLOYEE_WORK_SCHEDULE", "Corporate", "Employee Work Schedule", "1.0.0", "2026-07-15", true),
    DELETE_EMPLOYEE_WORK_SCHEDULE("DELETE_EMPLOYEE_WORK_SCHEDULE", "Corporate", "Employee Work Schedule", "1.0.0", "2026-07-15", true),

    // Employee Contact
    GET_EMPLOYEE_CONTACT("GET_EMPLOYEE_CONTACT", "Corporate", "Employee Contact", "1.0.0", "2026-07-09", true),
    CREATE_EMPLOYEE_CONTACT("CREATE_EMPLOYEE_CONTACT", "Corporate", "Employee Contact", "1.0.0", "2026-07-09", true),
    UPDATE_EMPLOYEE_CONTACT("UPDATE_EMPLOYEE_CONTACT", "Corporate", "Employee Contact", "1.0.0", "2026-07-09", true),
    DELETE_EMPLOYEE_CONTACT("DELETE_EMPLOYEE_CONTACT", "Corporate", "Employee Contact", "1.0.0", "2026-07-09", true),

    // Employee Address
    GET_EMPLOYEE_ADDRESS("GET_EMPLOYEE_ADDRESS", "Corporate", "Employee Address", "1.0.0", "2026-07-09", true),
    CREATE_EMPLOYEE_ADDRESS("CREATE_EMPLOYEE_ADDRESS", "Corporate", "Employee Address", "1.0.0", "2026-07-09", true),
    UPDATE_EMPLOYEE_ADDRESS("UPDATE_EMPLOYEE_ADDRESS", "Corporate", "Employee Address", "1.0.0", "2026-07-09", true),
    DELETE_EMPLOYEE_ADDRESS("DELETE_EMPLOYEE_ADDRESS", "Corporate", "Employee Address", "1.0.0", "2026-07-09", true),

    // Configuration
    GET_CONFIGURATION("GET_CONFIGURATION", "Configuration", "Configuration", "1.0.0", "2026-07-09", true),
    UPDATE_CONFIGURATION("UPDATE_CONFIGURATION", "Configuration", "Configuration", "1.0.0", "2026-07-09", true),

    // Address Type
    GET_ADDRESS_TYPE("GET_ADDRESS_TYPE", "Corporate", "Address Type", "1.0.0", "2026-07-09", true),
    CREATE_ADDRESS_TYPE("CREATE_ADDRESS_TYPE", "Corporate", "Address Type", "1.0.0", "2026-07-09", true),
    UPDATE_ADDRESS_TYPE("UPDATE_ADDRESS_TYPE", "Corporate", "Address Type", "1.0.0", "2026-07-09", true),
    ENABLE_ADDRESS_TYPE("ENABLE_ADDRESS_TYPE", "Corporate", "Address Type", "1.0.0", "2026-07-09", true),
    DISABLE_ADDRESS_TYPE("DISABLE_ADDRESS_TYPE", "Corporate", "Address Type", "1.0.0", "2026-07-09", true),

    // Contact Type
    GET_CONTACT_TYPE("GET_CONTACT_TYPE", "Corporate", "Contact Type", "1.0.0", "2026-07-09", true),
    CREATE_CONTACT_TYPE("CREATE_CONTACT_TYPE", "Corporate", "Contact Type", "1.0.0", "2026-07-09", true),
    UPDATE_CONTACT_TYPE("UPDATE_CONTACT_TYPE", "Corporate", "Contact Type", "1.0.0", "2026-07-09", true),
    ENABLE_CONTACT_TYPE("ENABLE_CONTACT_TYPE", "Corporate", "Contact Type", "1.0.0", "2026-07-09", true),
    DISABLE_CONTACT_TYPE("DISABLE_CONTACT_TYPE", "Corporate", "Contact Type", "1.0.0", "2026-07-09", true),

    // Cnae
    GET_CNAE("GET_CNAE", "Corporate", "Cnae", "1.0.0", "2026-07-09", true),
    CREATE_CNAE("CREATE_CNAE", "Corporate", "Cnae", "1.0.0", "2026-07-09", true),
    UPDATE_CNAE("UPDATE_CNAE", "Corporate", "Cnae", "1.0.0", "2026-07-09", true),
    DELETE_CNAE("DELETE_CNAE", "Corporate", "Cnae", "1.0.0", "2026-07-09", true),

    // Legal Nature
    GET_LEGAL_NATURE("GET_LEGAL_NATURE", "Corporate", "Legal Nature", "1.0.0", "2026-07-09", true),
    CREATE_LEGAL_NATURE("CREATE_LEGAL_NATURE", "Corporate", "Legal Nature", "1.0.0", "2026-07-09", true),
    UPDATE_LEGAL_NATURE("UPDATE_LEGAL_NATURE", "Corporate", "Legal Nature", "1.0.0", "2026-07-09", true),
    DELETE_LEGAL_NATURE("DELETE_LEGAL_NATURE", "Corporate", "Legal Nature", "1.0.0", "2026-07-09", true),

    // Reason Activate
    GET_REASON_ACTIVATE("GET_REASON_ACTIVATE", "Access", "Reason Activate", "1.0.0", "2026-07-09", true),
    CREATE_REASON_ACTIVATE("CREATE_REASON_ACTIVATE", "Access", "Reason Activate", "1.0.0", "2026-07-09", true),
    UPDATE_REASON_ACTIVATE("UPDATE_REASON_ACTIVATE", "Access", "Reason Activate", "1.0.0", "2026-07-09", true),
    ENABLE_REASON_ACTIVATE("ENABLE_REASON_ACTIVATE", "Access", "Reason Activate", "1.0.0", "2026-07-09", true),
    DISABLE_REASON_ACTIVATE("DISABLE_REASON_ACTIVATE", "Access", "Reason Activate", "1.0.0", "2026-07-09", true),

    // Reason Inactivate
    GET_REASON_INACTIVATE("GET_REASON_INACTIVATE", "Access", "Reason Inactivate", "1.0.0", "2026-07-09", true),
    CREATE_REASON_INACTIVATE("CREATE_REASON_INACTIVATE", "Access", "Reason Inactivate", "1.0.0", "2026-07-09", true),
    UPDATE_REASON_INACTIVATE("UPDATE_REASON_INACTIVATE", "Access", "Reason Inactivate", "1.0.0", "2026-07-09", true),
    ENABLE_REASON_INACTIVATE("ENABLE_REASON_INACTIVATE", "Access", "Reason Inactivate", "1.0.0", "2026-07-09", true),
    DISABLE_REASON_INACTIVATE("DISABLE_REASON_INACTIVATE", "Access", "Reason Inactivate", "1.0.0", "2026-07-09", true),

    // Reason Disable
    GET_REASON_DISABLE("GET_REASON_DISABLE", "Access", "Reason Disable", "1.0.0", "2026-07-09", true),
    CREATE_REASON_DISABLE("CREATE_REASON_DISABLE", "Access", "Reason Disable", "1.0.0", "2026-07-09", true),
    UPDATE_REASON_DISABLE("UPDATE_REASON_DISABLE", "Access", "Reason Disable", "1.0.0", "2026-07-09", true),
    ENABLE_REASON_DISABLE("ENABLE_REASON_DISABLE", "Access", "Reason Disable", "1.0.0", "2026-07-09", true),
    DISABLE_REASON_DISABLE("DISABLE_REASON_DISABLE", "Access", "Reason Disable", "1.0.0", "2026-07-09", true),

    // Reason Enable
    GET_REASON_ENABLE("GET_REASON_ENABLE", "Access", "Reason Enable", "1.0.0", "2026-07-09", true),
    CREATE_REASON_ENABLE("CREATE_REASON_ENABLE", "Access", "Reason Enable", "1.0.0", "2026-07-09", true),
    UPDATE_REASON_ENABLE("UPDATE_REASON_ENABLE", "Access", "Reason Enable", "1.0.0", "2026-07-09", true),
    ENABLE_REASON_ENABLE("ENABLE_REASON_ENABLE", "Access", "Reason Enable", "1.0.0", "2026-07-09", true),
    DISABLE_REASON_ENABLE("DISABLE_REASON_ENABLE", "Access", "Reason Enable", "1.0.0", "2026-07-09", true),

    // Reason Position Change
    GET_REASON_POSITION_CHANGE("GET_REASON_POSITION_CHANGE", "Access", "Reason Position Change", "1.0.0", "2026-07-15", true),
    CREATE_REASON_POSITION_CHANGE("CREATE_REASON_POSITION_CHANGE", "Access", "Reason Position Change", "1.0.0", "2026-07-15", true),
    UPDATE_REASON_POSITION_CHANGE("UPDATE_REASON_POSITION_CHANGE", "Access", "Reason Position Change", "1.0.0", "2026-07-15", true),
    ENABLE_REASON_POSITION_CHANGE("ENABLE_REASON_POSITION_CHANGE", "Access", "Reason Position Change", "1.0.0", "2026-07-15", true),
    DISABLE_REASON_POSITION_CHANGE("DISABLE_REASON_POSITION_CHANGE", "Access", "Reason Position Change", "1.0.0", "2026-07-15", true),

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
