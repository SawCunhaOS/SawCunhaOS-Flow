
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

    // Employee
    SCOS_EMPLOYEE_001("SCOS_EMPLOYEE_001", 422, "SCOS_TITLE_BUSINESS_RULE_VIOLATION"),

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

    ;

    /** Código estável, usado como chave de mensagem para o {@code detail} da resposta. */
    private final String code;
    /** Status HTTP da resposta ({@link br.com.sawcunhaos.foundation.exception.ExceptionsHandler}). */
    private final int httpCode;
    /** Chave de mensagem (categórica) para o {@code title} RFC 9457. */
    private final String title;
}
