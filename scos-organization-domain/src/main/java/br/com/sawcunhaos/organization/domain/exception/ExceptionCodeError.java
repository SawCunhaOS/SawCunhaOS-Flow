package br.com.sawcunhaos.organization.domain.exception;

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

    // Position
    SCOS_POSITION_001("SCOS_POSITION_001"),
    SCOS_POSITION_002("SCOS_POSITION_002"),
    SCOS_POSITION_003("SCOS_POSITION_003"),

    // Company
    SCOS_COMPANY_001("SCOS_COMPANY_001"),
    SCOS_COMPANY_002("SCOS_COMPANY_002"),
    SCOS_COMPANY_003("SCOS_COMPANY_003"),
    SCOS_COMPANY_004("SCOS_COMPANY_004"),


    // User / Login
    SCOS_USER_001("SCOS_USER_001"),
    SCOS_USER_002("SCOS_USER_002"),
    SCOS_USER_003("SCOS_USER_003"),
    SCOS_USER_004("SCOS_USER_004");

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
