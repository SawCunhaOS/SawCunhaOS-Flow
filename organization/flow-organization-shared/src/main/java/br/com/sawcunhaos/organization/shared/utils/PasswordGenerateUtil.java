
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Objects;

@Component
@Slf4j
public final class PasswordGenerateUtil {

    @Value("${scos.security.password.salt.length:24}")
    private int saltLength;
    @Value("${scos.security.password.algorithm:PBKDF2WithHmacSHA512}")
    private String algorithm;
    @Value("${scos.security.password.key.length:512}")
    private int keyLength;
    @Value("${scos.security.password.iterations:210000}")
    private int interations;

    private SecureRandom RANDOM_SALT = new SecureRandom();
    private SecretKeyFactory FACTORY_PBKDF2;

    private SecretKeyFactory getSecretKeyFactory() {
        if (Objects.isNull(FACTORY_PBKDF2)) {
            try {
                FACTORY_PBKDF2 = SecretKeyFactory.getInstance(algorithm);
            } catch (Exception e) {
                log.error("Error on generate secret key factory", e);
            }
        }
        return FACTORY_PBKDF2;
    }

    public byte[] generateSalt() {
        byte[] salt = new byte[saltLength];
        RANDOM_SALT.nextBytes(salt);
        return salt;
    }

    public byte[] generatePasswordHash(String password, byte[] salt) throws InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, interations, keyLength);

        return getSecretKeyFactory().generateSecret(spec).getEncoded();
    }

    public String convertByteToString(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

}
