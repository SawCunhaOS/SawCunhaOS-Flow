
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

package br.com.sawcunhaos.organization.shared.utils;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Instancia {@link SystemSecretCryptoService} sem contexto Spring — campos {@code @Value} setados
 * via reflection, {@code init()} (package-private) chamado direto por estar no mesmo pacote.
 */
class SystemSecretCryptoServiceTest {

    private static final String MASTER_KEY_A = masterKey((byte) 1);
    private static final String MASTER_KEY_B = masterKey((byte) 2);
    private static final int IV_LENGTH = 12;

    private static String masterKey(byte fill) {
        byte[] key = new byte[32];
        Arrays.fill(key, fill);
        return Base64.getEncoder().encodeToString(key);
    }

    private static SystemSecretCryptoService newService(String masterKeyBase64) throws Exception {
        SystemSecretCryptoService service = new SystemSecretCryptoService();
        setField(service, "masterKeyBase64", masterKeyBase64);
        service.init();
        return service;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SystemSecretCryptoService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void encryptThenDecryptReturnsOriginalValue() throws Exception {
        SystemSecretCryptoService service = newService(MASTER_KEY_A);

        String encrypted = service.encrypt("valor-secreto");

        assertEquals("valor-secreto", service.decrypt(encrypted));
    }

    @Test
    void twoEncryptCallsProduceDifferentIvAndCiphertext() throws Exception {
        SystemSecretCryptoService service = newService(MASTER_KEY_A);

        String first = service.encrypt("valor-secreto");
        String second = service.encrypt("valor-secreto");

        assertNotEquals(first, second);

        byte[] firstBytes = Base64.getDecoder().decode(first);
        byte[] secondBytes = Base64.getDecoder().decode(second);
        byte[] firstIv = Arrays.copyOfRange(firstBytes, 0, IV_LENGTH);
        byte[] secondIv = Arrays.copyOfRange(secondBytes, 0, IV_LENGTH);

        assertFalse(Arrays.equals(firstIv, secondIv));
    }

    @Test
    void decryptWithDifferentMasterKeyThrows() throws Exception {
        SystemSecretCryptoService serviceA = newService(MASTER_KEY_A);
        SystemSecretCryptoService serviceB = newService(MASTER_KEY_B);
        String encrypted = serviceA.encrypt("valor-secreto");

        assertThrows(ScosException.class, () -> serviceB.decrypt(encrypted));
    }

    @Test
    void decryptWithCorruptedPayloadThrows() throws Exception {
        SystemSecretCryptoService service = newService(MASTER_KEY_A);
        String encrypted = service.encrypt("valor-secreto");
        byte[] data = Base64.getDecoder().decode(encrypted);
        data[data.length - 1] ^= 0xFF;
        String corrupted = Base64.getEncoder().encodeToString(data);

        assertThrows(ScosException.class, () -> service.decrypt(corrupted));
    }
}
