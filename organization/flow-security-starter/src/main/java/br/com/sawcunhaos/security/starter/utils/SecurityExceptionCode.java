package br.com.sawcunhaos.security.starter.utils;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum SecurityExceptionCode implements ExceptionCode {

    SCOS_AUTH_001("SCOS_AUTH_001"),
    SCOS_AUTH_002("SCOS_AUTH_002"),
    SCOS_AUTH_003("SCOS_AUTH_003"),
    SCOS_AUTH_004("SCOS_AUTH_004"),
    SCOS_AUTH_005("SCOS_AUTH_005");

    private final String code;

}
