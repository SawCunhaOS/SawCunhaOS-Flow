
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
 */
public enum ExceptionCodeError implements ExceptionCode {

    // Department
    SCOS_DEPARTMENT_001("SCOS_DEPARTMENT_001"),
    SCOS_DEPARTMENT_002("SCOS_DEPARTMENT_002"),
    SCOS_DEPARTMENT_003("SCOS_DEPARTMENT_003"),
    /** Department já está ativo — impossível ativar novamente. HTTP 422. */
    SCOS_DEPARTMENT_004("SCOS_DEPARTMENT_004"),
    /** Department já está inativo — impossível inativar novamente. HTTP 422. */
    SCOS_DEPARTMENT_005("SCOS_DEPARTMENT_005"),
    /** Department referenciado está inativo — Position não pode ser criada/atualizada. HTTP 422. */
    SCOS_DEPARTMENT_006("SCOS_DEPARTMENT_006"),

    // Position
    SCOS_POSITION_001("SCOS_POSITION_001"),
    SCOS_POSITION_002("SCOS_POSITION_002"),
    SCOS_POSITION_003("SCOS_POSITION_003"),
    /** Position já está ativa — impossível ativar novamente. HTTP 422. */
    SCOS_POSITION_004("SCOS_POSITION_004"),
    /** Position já está inativa — impossível inativar novamente. HTTP 422. */
    SCOS_POSITION_005("SCOS_POSITION_005"),

    // Company
    SCOS_COMPANY_001("SCOS_COMPANY_001"),
    SCOS_COMPANY_002("SCOS_COMPANY_002"),
    SCOS_COMPANY_003("SCOS_COMPANY_003"),
    SCOS_COMPANY_004("SCOS_COMPANY_004"),
    SCOS_COMPANY_005("SCOS_COMPANY_005"),
    SCOS_COMPANY_006("SCOS_COMPANY_006"),
    SCOS_COMPANY_007("SCOS_COMPANY_007"),

    // Configuration
    /** Configuração não encontrada pelo id informado. HTTP 400. */
    SCOS_CONFIGURATION_001("SCOS_CONFIGURATION_001"),
    /** Valor incompatível com o tipo declarado da configuração. HTTP 400. */
    SCOS_CONFIGURATION_002("SCOS_CONFIGURATION_002"),

    // Employee
    SCOS_EMPLOYEE_001("SCOS_EMPLOYEE_001"),

    // User / Login
    SCOS_USER_001("SCOS_USER_001"),
    SCOS_USER_002("SCOS_USER_002"),
    SCOS_USER_003("SCOS_USER_003"),
    SCOS_USER_004("SCOS_USER_004"),

    SCOS_AUTHORITY_001("SCOS_AUTHORITY_001"),

    SCOS_LOGIN_001("SCOS_LOGIN_001"),
    SCOS_LOGIN_002("SCOS_LOGIN_002"),
    SCOS_LOGIN_003("SCOS_LOGIN_003"),
    SCOS_LOGIN_010("SCOS_LOGIN_010"),
    SCOS_LOGIN_011("SCOS_LOGIN_011"),
    /** Transição de status inválida solicitada para o Login informado. HTTP 422. */
    SCOS_LOGIN_013("SCOS_LOGIN_013"),


    ;

    private final String code;

    // Regex pattern to validate the error-code format
    public static final String CODE_PATTERN = "^SCOS_[A-Z0-9]+_\\d{3}$";

    /**
     * Valida se o código segue o padrão definido pelo projeto.
     */
    public static boolean isValidCode(String code) {
        return code != null && code.matches(CODE_PATTERN);
    }
}
