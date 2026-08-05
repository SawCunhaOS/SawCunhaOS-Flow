
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Organization
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.organization.shared.exception;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
/**
 * Códigos de erro do domínio.
 * Padrão: SCOS_<MÓDULO>_<NNN> —
 *  - Prefixo: `SCOS_`
 *  - Módulo / funcionalidade: letras maiúsculas (ex.: DEPARTMENT, COMPANY, POSITION)
 *  - Sufixo: número incremental de 3 dígitos com underscore (ex.: _001)
 * Exemplo: `SCOS_DEPARTMENT_001`
 *
 * <p>Cada constante também carrega {@code httpCode} (status HTTP da resposta) e {@code title}
 * (chave de mensagem RFC 9457, categórica — ver {@code SCOS_TITLE_*} no bundle de mensagens).
 */
public enum ExceptionCodeError implements ExceptionCode {

    // Title categories (RFC 9457) — chaves de mensagem, resolvidas via LocaleService.getMessage
    // SCOS_TITLE_NOT_FOUND=404, SCOS_TITLE_CONFLICT=409, SCOS_TITLE_BUSINESS_RULE_VIOLATION=422,
    // SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE=502, SCOS_TITLE_INTERNAL_ERROR=500,
    // SCOS_TITLE_UNAUTHORIZED=401, SCOS_TITLE_GENERIC=400 (default/órfãos)

    //Configuration
    SCOS_CONFIGURATION_001("SCOS_CONFIGURATION_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_CONFIGURATION_002("SCOS_CONFIGURATION_002", 404, "SCOS_TITLE_NOT_FOUND"),

    // System
    /** Sistema informado no registro de resources não existe. HTTP 404. */
    SCOS_SYSTEM_001("SCOS_SYSTEM_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_SYSTEM_002("SCOS_SYSTEM_002", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Department
    SCOS_DEPARTMENT_001("SCOS_DEPARTMENT_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_DEPARTMENT_002("SCOS_DEPARTMENT_002", 409, "SCOS_TITLE_CONFLICT"),
    SCOS_DEPARTMENT_003("SCOS_DEPARTMENT_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Department já está ativo — impossível ativar novamente. HTTP 422. */
    SCOS_DEPARTMENT_004("SCOS_DEPARTMENT_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Department já está inativo — impossível inativar novamente. HTTP 422. */
    SCOS_DEPARTMENT_005("SCOS_DEPARTMENT_005", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Department referenciado está inativo — Position não pode ser criada/atualizada. HTTP 422. */
    SCOS_DEPARTMENT_006("SCOS_DEPARTMENT_006", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Position
    SCOS_POSITION_001("SCOS_POSITION_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_POSITION_002("SCOS_POSITION_002", 409, "SCOS_TITLE_CONFLICT"),
    SCOS_POSITION_003("SCOS_POSITION_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Position já está ativa — impossível ativar novamente. HTTP 422. */
    SCOS_POSITION_004("SCOS_POSITION_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Position já está inativa — impossível inativar novamente. HTTP 422. */
    SCOS_POSITION_005("SCOS_POSITION_005", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Position Work Schedule
    /** Não existe horário cadastrado para este cargo neste dia da semana. HTTP 404. */
    SCOS_POSITION_WORK_SCHEDULE_001("SCOS_POSITION_WORK_SCHEDULE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Já existe um horário cadastrado para este cargo neste dia da semana. HTTP 409. */
    SCOS_POSITION_WORK_SCHEDULE_002("SCOS_POSITION_WORK_SCHEDULE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Ordem cronológica inválida: startTime < lunchStart < lunchEnd < endTime. HTTP 422. */
    SCOS_POSITION_WORK_SCHEDULE_003("SCOS_POSITION_WORK_SCHEDULE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Company
    SCOS_COMPANY_001("SCOS_COMPANY_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_COMPANY_002("SCOS_COMPANY_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Sem throw site hoje — reservado para bloqueio de empresa com colaboradores ativos. HTTP 422. */
    SCOS_COMPANY_003("SCOS_COMPANY_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    SCOS_COMPANY_004("SCOS_COMPANY_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    SCOS_COMPANY_005("SCOS_COMPANY_005", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Sem throw site hoje — reservado para bloqueio da única empresa ativa. HTTP 422. */
    SCOS_COMPANY_006("SCOS_COMPANY_006", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    SCOS_COMPANY_007("SCOS_COMPANY_007", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de ativação informado está inativo. HTTP 422. */
    SCOS_COMPANY_008("SCOS_COMPANY_008", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de ativação incompatível com a entidade Empresa (entityType != COMPANY). HTTP 422. */
    SCOS_COMPANY_009("SCOS_COMPANY_009", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Empresa mãe informada não está ativa. HTTP 422. */
    SCOS_COMPANY_010("SCOS_COMPANY_010", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Profundidade da hierarquia de empresas excede COMPANY_HIERARCHY_MAX_DEPTH. HTTP 422. */
    SCOS_COMPANY_011("SCOS_COMPANY_011", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de inativação informado está inativo. HTTP 422. */
    SCOS_COMPANY_012("SCOS_COMPANY_012", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de inativação informado é incompatível com a entidade Empresa. HTTP 422. */
    SCOS_COMPANY_013("SCOS_COMPANY_013", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio informado está inativo. HTTP 422. */
    SCOS_COMPANY_014("SCOS_COMPANY_014", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio informado é incompatível com a entidade Empresa. HTTP 422. */
    SCOS_COMPANY_015("SCOS_COMPANY_015", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio informado está inativo. HTTP 422. */
    SCOS_COMPANY_016("SCOS_COMPANY_016", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio informado é incompatível com a entidade Empresa. HTTP 422. */
    SCOS_COMPANY_017("SCOS_COMPANY_017", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Não é possível desativar/bloquear a empresa: existe filial ativa em algum nível da subárvore. HTTP 422. */
    SCOS_COMPANY_018("SCOS_COMPANY_018", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Employee
    SCOS_EMPLOYEE_001("SCOS_EMPLOYEE_001", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** CPF já cadastrado em outro funcionário (qualquer status). HTTP 409. */
    SCOS_EMPLOYEE_002("SCOS_EMPLOYEE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** E-mail já cadastrado em outro funcionário (qualquer status). HTTP 409. */
    SCOS_EMPLOYEE_003("SCOS_EMPLOYEE_003", 409, "SCOS_TITLE_CONFLICT"),
    /** Supervisor informado não corresponde a nenhum funcionário existente. HTTP 404. */
    SCOS_EMPLOYEE_004("SCOS_EMPLOYEE_004", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Empresa informada não está ACTIVE. HTTP 422. */
    SCOS_EMPLOYEE_005("SCOS_EMPLOYEE_005", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Cargo informado está inativo (active=false). HTTP 422. */
    SCOS_EMPLOYEE_006("SCOS_EMPLOYEE_006", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Supervisor informado não está ACTIVE. HTTP 422. */
    SCOS_EMPLOYEE_007("SCOS_EMPLOYEE_007", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de ativação informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_008("SCOS_EMPLOYEE_008", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de ativação incompatível com a entidade Funcionário (entityType != EMPLOYEE). HTTP 422. */
    SCOS_EMPLOYEE_009("SCOS_EMPLOYEE_009", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Domínio do e-mail não corresponde a EMPLOYEE_EMAIL_DOMAIN. HTTP 422. */
    SCOS_EMPLOYEE_010("SCOS_EMPLOYEE_010", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Idade em dateOfHiring é menor que EMPLOYEE_MIN_AGE. HTTP 422. */
    SCOS_EMPLOYEE_011("SCOS_EMPLOYEE_011", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Data de nascimento no futuro. HTTP 422. */
    SCOS_EMPLOYEE_012("SCOS_EMPLOYEE_012", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Data de admissão anterior à data de nascimento. HTTP 422. */
    SCOS_EMPLOYEE_013("SCOS_EMPLOYEE_013", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Funcionário informado não encontrado. HTTP 404. */
    SCOS_EMPLOYEE_014("SCOS_EMPLOYEE_014", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Motivo de inativação informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_015("SCOS_EMPLOYEE_015", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de inativação incompatível com a entidade Funcionário (entityType != EMPLOYEE). HTTP 422. */
    SCOS_EMPLOYEE_016("SCOS_EMPLOYEE_016", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_017("SCOS_EMPLOYEE_017", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio incompatível com a entidade Funcionário. HTTP 422. */
    SCOS_EMPLOYEE_018("SCOS_EMPLOYEE_018", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_019("SCOS_EMPLOYEE_019", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio incompatível com a entidade Funcionário. HTTP 422. */
    SCOS_EMPLOYEE_020("SCOS_EMPLOYEE_020", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Nenhum Funcionário INACTIVE encontrado para o CPF informado (nunca existiu, ou pertence a ACTIVE/DISABLED). HTTP 404. */
    SCOS_EMPLOYEE_021("SCOS_EMPLOYEE_021", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Motivo de mudança de cargo (reasonPositionChangeId) informado não encontrado. HTTP 404. */
    SCOS_EMPLOYEE_022("SCOS_EMPLOYEE_022", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Motivo de mudança de cargo informado está inativo. HTTP 422. */
    SCOS_EMPLOYEE_023("SCOS_EMPLOYEE_023", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // User / Login
    SCOS_USER_001("SCOS_USER_001", 502, "SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE"),
    SCOS_USER_002("SCOS_USER_002", 502, "SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE"),
    SCOS_USER_003("SCOS_USER_003", 502, "SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE"),
    SCOS_USER_004("SCOS_USER_004", 500, "SCOS_TITLE_INTERNAL_ERROR"),

    SCOS_AUTHORITY_001("SCOS_AUTHORITY_001", 404, "SCOS_TITLE_NOT_FOUND"),

    // Address Type
    SCOS_ADDRESS_TYPE_001("SCOS_ADDRESS_TYPE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_ADDRESS_TYPE_002("SCOS_ADDRESS_TYPE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Tipo de endereço já está ativo — impossível ativar novamente. HTTP 422. */
    SCOS_ADDRESS_TYPE_003("SCOS_ADDRESS_TYPE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Tipo de endereço já está inativo — impossível inativar novamente. HTTP 422. */
    SCOS_ADDRESS_TYPE_004("SCOS_ADDRESS_TYPE_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Contact Type
    SCOS_CONTACT_TYPE_001("SCOS_CONTACT_TYPE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_CONTACT_TYPE_002("SCOS_CONTACT_TYPE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Tipo de contato já está ativo — impossível ativar novamente. HTTP 422. */
    SCOS_CONTACT_TYPE_003("SCOS_CONTACT_TYPE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Tipo de contato já está inativo — impossível inativar novamente. HTTP 422. */
    SCOS_CONTACT_TYPE_004("SCOS_CONTACT_TYPE_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Cnae
    /** CNAE não encontrado. HTTP 404. */
    SCOS_CNAE_001("SCOS_CNAE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Já existe um CNAE com o mesmo code. HTTP 409. */
    SCOS_CNAE_002("SCOS_CNAE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** CNAE ainda vinculado a empresa (principal ou secundário) — não pode ser excluído. HTTP 422. */
    SCOS_CNAE_003("SCOS_CNAE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Legal Nature
    /** Natureza jurídica não encontrada. HTTP 404. */
    SCOS_LEGAL_NATURE_001("SCOS_LEGAL_NATURE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    /** Já existe uma natureza jurídica com o mesmo code. HTTP 409. */
    SCOS_LEGAL_NATURE_002("SCOS_LEGAL_NATURE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Natureza jurídica ainda vinculada a empresa — não pode ser excluída. HTTP 422. */
    SCOS_LEGAL_NATURE_003("SCOS_LEGAL_NATURE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Reason Activate
    SCOS_REASON_ACTIVATE_001("SCOS_REASON_ACTIVATE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_REASON_ACTIVATE_002("SCOS_REASON_ACTIVATE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Motivo de ativação já está ativo — impossível ativar novamente. HTTP 422. */
    SCOS_REASON_ACTIVATE_003("SCOS_REASON_ACTIVATE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de ativação já está inativo — impossível inativar novamente. HTTP 422. */
    SCOS_REASON_ACTIVATE_004("SCOS_REASON_ACTIVATE_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Reason Inactivate
    SCOS_REASON_INACTIVATE_001("SCOS_REASON_INACTIVATE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_REASON_INACTIVATE_002("SCOS_REASON_INACTIVATE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Motivo de inativação já está ativo — impossível ativar novamente. HTTP 422. */
    SCOS_REASON_INACTIVATE_003("SCOS_REASON_INACTIVATE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de inativação já está inativo — impossível inativar novamente. HTTP 422. */
    SCOS_REASON_INACTIVATE_004("SCOS_REASON_INACTIVATE_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Reason Disable
    SCOS_REASON_DISABLE_001("SCOS_REASON_DISABLE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_REASON_DISABLE_002("SCOS_REASON_DISABLE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Motivo de bloqueio já está ativo — impossível ativar novamente. HTTP 422. */
    SCOS_REASON_DISABLE_003("SCOS_REASON_DISABLE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de bloqueio já está inativo — impossível inativar novamente. HTTP 422. */
    SCOS_REASON_DISABLE_004("SCOS_REASON_DISABLE_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    // Reason Enable
    SCOS_REASON_ENABLE_001("SCOS_REASON_ENABLE_001", 404, "SCOS_TITLE_NOT_FOUND"),
    SCOS_REASON_ENABLE_002("SCOS_REASON_ENABLE_002", 409, "SCOS_TITLE_CONFLICT"),
    /** Motivo de desbloqueio já está ativo — impossível ativar novamente. HTTP 422. */
    SCOS_REASON_ENABLE_003("SCOS_REASON_ENABLE_003", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),
    /** Motivo de desbloqueio já está inativo — impossível inativar novamente. HTTP 422. */
    SCOS_REASON_ENABLE_004("SCOS_REASON_ENABLE_004", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    /** Órfão — sem mensagem PT/EN, sem throw site. Mantido reservado com default. */
    SCOS_LOGIN_001("SCOS_LOGIN_001", 400, "SCOS_TITLE_GENERIC"),
    /** Órfão — sem mensagem PT/EN, sem throw site. Mantido reservado com default. */
    SCOS_LOGIN_002("SCOS_LOGIN_002", 400, "SCOS_TITLE_GENERIC"),
    /** Órfão — sem mensagem PT/EN, sem throw site. Mantido reservado com default. */
    SCOS_LOGIN_003("SCOS_LOGIN_003", 400, "SCOS_TITLE_GENERIC"),
    /** Sem throw site hoje — reservado para rejeição de autenticação com login inativo. HTTP 401. */
    SCOS_LOGIN_010("SCOS_LOGIN_010", 401, "SCOS_TITLE_UNAUTHORIZED"),
    /** Sem throw site hoje — reservado para rejeição de autenticação com login bloqueado. HTTP 401. */
    SCOS_LOGIN_011("SCOS_LOGIN_011", 401, "SCOS_TITLE_UNAUTHORIZED"),
    /** Transição de status inválida solicitada para o Login informado. HTTP 422. */
    SCOS_LOGIN_013("SCOS_LOGIN_013", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

    SCOS_SECURITY_ENCRYPT("SCOS_SECURITY_ENCRYPT", 400, "SCOS_TITLE_GENERIC"),
    SCOS_SECURITY_DECRYPT("SCOS_SECURITY_DECRYPT", 400, "SCOS_TITLE_GENERIC"),

    ;

    /** Código estável, usado como chave de mensagem para o {@code detail} da resposta. */
    private final String code;
    /** Status HTTP da resposta ({@link br.com.sawcunhaos.foundation.exception.ExceptionsHandler}). */
    private final int httpCode;
    /** Chave de mensagem (categórica) para o {@code title} RFC 9457. */
    private final String title;
}
