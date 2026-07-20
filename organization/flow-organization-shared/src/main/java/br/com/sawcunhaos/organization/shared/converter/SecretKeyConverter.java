
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

package br.com.sawcunhaos.organization.shared.converter;

import br.com.sawcunhaos.organization.shared.utils.SystemSecretCryptoService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Converter
@RequiredArgsConstructor
public class SecretKeyConverter implements AttributeConverter<String, String> {
    private final SystemSecretCryptoService crypto;
    @Override public String convertToDatabaseColumn(String raw) {
        return raw == null ? null : crypto.encrypt(raw);
    }
    @Override public String convertToEntityAttribute(String enc) {
        return enc == null ? null : crypto.decrypt(enc);
    }
}
